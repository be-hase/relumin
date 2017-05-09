package com.be_hase.relumin.support.redis;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import com.be_hase.relumin.model.ClusterNode;
import com.be_hase.relumin.model.CreateClusterParam;
import com.be_hase.relumin.support.ValidationUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.lambdaworks.redis.MigrateArgs;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Almost clone of redis-trib.rb
 * https://github.com/antirez/redis/blob/unstable/src/redis-trib.rb
 */
@Slf4j
public class RedisTrib implements AutoCloseable {
    private int replicas;
    private boolean fix;

    @Getter
    private final List<String> errors = Lists.newArrayList();

    @Getter
    private final List<TribClusterNode> nodes = Lists.newArrayList();

    public List<CreateClusterParam> getCreateClusterParams(final int replicas, final Set<String> hostAndPorts) {
        this.replicas = replicas;

        for (String hostAndPort : hostAndPorts) {
            hostAndPort = StringUtils.trim(hostAndPort);
            final TribClusterNode node = createTribClusterNode(hostAndPort);
            validateClusterAndEmptyNode(node);
            addNodes(node);
        }

        checkCreateParameters();
        allocSlots();

        return buildCreateClusterParam();
    }

    public void createCluster(final List<CreateClusterParam> params) throws Exception {
        ValidationUtils.createClusterParams(params);

        log.info("Creating cluster.");
        // set nodes
        params.forEach(param -> {
            final TribClusterNode masterNode = createTribClusterNode(param.getMaster());
            validateClusterAndEmptyNode(masterNode);
            addNodes(masterNode);

            param.getReplicas().forEach(replica -> {
                final TribClusterNode replicaNode = createTribClusterNode(replica);
                validateClusterAndEmptyNode(replicaNode);
                replicaNode.setAsReplica(masterNode.getNode().getNodeId());
                addNodes(replicaNode);
            });

            final int startSlot = Integer.valueOf(param.getStartSlotNumber());
            final int endSlot = Integer.valueOf(param.getEndSlotNumber());
            final List<Integer> tmpSlots = IntStream.rangeClosed(startSlot, endSlot)
                                                    .boxed()
                                                    .collect(Collectors.toList());
            masterNode.addTmpSlots(tmpSlots);
        });

        // flush nodes config
        flushNodesConfig();
        log.info("Nodes configuration updated.");
        log.info("Assign a different config epoch to each node.");
        assignConfigEpoch();
        log.info("Sending CLUSTER MEET messages to join the cluster.");
        joinCluster();
        // Give one second for the join to start, in order to avoid that
        // wait_cluster_join will find all the nodes agree about the config as
        // they are still empty with unassigned slots.
        Thread.sleep(1000);
        waitClusterJoin();
        flushNodesConfig(); // Useful for the replicas.

        // Reset the node information, so that when the
        // final summary is listed in check_cluster about the newly created cluster
        // all the nodes would get properly listed as slaves or masters
        nodes.clear();
        loadClusterInfoFromNode(params.get(0).getMaster());
        checkCluster();
    }

    public List<String> checkCluster(final String hostAndPort) {
        loadClusterInfoFromNode(hostAndPort);
        return checkCluster();
    }

    public void fixCluster(final String hostAndPort) throws Exception {
        fix = true;
        loadClusterInfoFromNode(hostAndPort);
        checkCluster();
    }

    public void reshardCluster(final String hostAndPort, final int slotCount, final String fromNodeIds,
                               final String toNodeId)
            throws Exception {
        loadClusterInfoFromNode(hostAndPort);
        checkCluster();
        if (!errors.isEmpty()) {
            throw new RedisTribException("Please fix your cluster problems before resharding.");
        }

        // Get the target instance
        final TribClusterNode target = getNodeByNodeId(toNodeId);
        if (target == null || target.hasFlag("slave")) {
            throw new IllegalArgumentException(
                    "The specified node is not known or not a master, please retry.");
        }

        // Get the source instance
        final List<TribClusterNode> sources = Lists.newArrayList();
        if (StringUtils.equals("ALL", fromNodeIds)) {
            for (final TribClusterNode node : nodes) {
                if (StringUtils.equals(node.getNode().getNodeId(), target.getNode().getNodeId())) {
                    continue;
                }
                if (node.hasFlag("slave")) {
                    continue;
                }
                sources.add(node);
            }
        } else {
            for (final String fromNodeId : StringUtils.split(fromNodeIds, ",")) {
                final TribClusterNode fromNode = getNodeByNodeId(fromNodeId);
                if (fromNode == null || fromNode.hasFlag("slave")) {
                    throw new IllegalArgumentException(String.format(
                            "The specified node(%s) is not known or is not a master, please retry.",
                            fromNodeId));
                }
                sources.add(fromNode);
            }
        }

        // Check if the destination node is the same of any source nodes.
        if (sources.stream()
                   .anyMatch(source -> StringUtils.equals(source.getNode().getNodeId(),
                                                          target.getNode().getNodeId()))) {
            throw new IllegalArgumentException("Target node is also listed among the source nodes!");
        }

        log.info("Ready to move {} slots.", slotCount);
        log.info("Source nodes : {}",
                 sources.stream()
                        .map(v -> String.format("%s (%s)",
                                                v.getNode().getHostAndPort(), v.getNode().getNodeId()))
                        .collect(Collectors.toList()));
        log.info("Destination nodes : {} ({})", target, target.getNode().getNodeId());
        final List<ReshardTable> reshardTables = computeReshardTable(sources, slotCount);
        reshardTables.forEach(
                reshardTable -> moveSlot(reshardTable.getSource(), target, reshardTable.getSlot(), null));
    }

    public void reshardClusterBySlots(final String hostAndPort, final Set<Integer> slots, final String toNodeId)
            throws Exception {
        loadClusterInfoFromNode(hostAndPort);
        checkCluster();
        if (!errors.isEmpty()) {
            throw new RedisTribException("Please fix your cluster problems before resharding.");
        }

        final TribClusterNode target = getNodeByNodeId(toNodeId);
        if (target == null || target.hasFlag("slave")) {
            throw new IllegalArgumentException(
                    "The specified node is not known or is not a master, please retry.");
        }

        final List<ReshardTable> reshardTables = Lists.newArrayList();
        final Set<Integer> notFoundSlot = Sets.newHashSet();
        for (final Integer slot : slots) {
            boolean found = false;

            if (target.getNode().getServedSlotsSet().contains(slot)) {
                continue;
            }
            for (final TribClusterNode node : nodes) {
                if (StringUtils.equals(node.getNode().getNodeId(), target.getNode().getNodeId())) {
                    continue;
                }
                if (node.hasFlag("slave")) {
                    continue;
                }
                if (node.getNode().getServedSlotsSet().contains(slot)) {
                    found = true;
                    reshardTables.add(new ReshardTable(node, slot));
                    break;
                }
            }

            if (!found) {
                notFoundSlot.add(slot);
            }
        }

        if (!notFoundSlot.isEmpty()) {
            throw new IllegalArgumentException(String.format("Cannot find the nodes which has slots(%s).",
                                                             new RedisSupport().slotsDisplay(notFoundSlot)));
        }

        reshardTables.forEach(
                reshardTable -> moveSlot(reshardTable.getSource(), target, reshardTable.getSlot(), null));
    }

    public void addNodeIntoCluster(final String hostAndPort, final String newHostAndPort) throws Exception {
        log.info("Adding node {} to cluster {}", newHostAndPort, hostAndPort);
        loadClusterInfoFromNode(hostAndPort);
        checkCluster();

        // Add the new node
        final TribClusterNode newNode = createTribClusterNode(newHostAndPort);
        validateClusterAndEmptyNode(newNode);
        addNodes(newNode);

        // Send CLUSTER MEET command to the new node
        final TribClusterNode node = getNodeByHostAndPort(hostAndPort);
        log.info("Send CLUSTER MEET to node {} to make it join the cluster.", newHostAndPort);
        newNode.getRedisCommands().clusterMeet(node.getNode().getHost(), node.getNode().getPort());

        log.info("New node added correctly.");
    }

    public void addNodeIntoClusterAsReplica(final String hostAndPort, final String newHostAndPort,
                                            final String materNodeId)
            throws Exception {
        log.info("Adding node {} to cluster {}", newHostAndPort, hostAndPort);
        loadClusterInfoFromNode(hostAndPort);
        checkCluster();

        // If --master-id was specified, try to resolve it now so that we
        // abort before starting with the node configuration.
        final TribClusterNode master;
        if (StringUtils.equals(materNodeId, "RANDOM")) {
            master = getMasterWithLeastReplicas();
            log.info("Automatically selected master {}({}).", master, master.getNode().getNodeId());
        } else {
            master = getNodeByNodeId(materNodeId);
            if (master == null) {
                throw new IllegalArgumentException(String.format("No such master ID %s", materNodeId));
            }
        }

        // Add the new node
        final TribClusterNode newNode = createTribClusterNode(newHostAndPort);
        validateClusterAndEmptyNode(newNode);
        addNodes(newNode);

        // Send CLUSTER MEET command to the new node
        final TribClusterNode node = getNodeByHostAndPort(hostAndPort);
        log.info("Send CLUSTER MEET to node {} to make it join the cluster.", newHostAndPort);
        newNode.getRedisCommands().clusterMeet(node.getNode().getHost(), node.getNode().getPort());

        // Additional configuration is needed if the node is added as
        // a slave.
        waitClusterJoin();
        log.info("Configure node as replica of {}.", master.getNode().getNodeId());
        newNode.getRedisCommands().clusterReplicate(master.getNode().getNodeId());

        log.info("New node added correctly.");
    }

    public void deleteNodeOfCluster(final String hostAndPort, final String nodeId, final String reset,
                                    final boolean shutdown)
            throws Exception {
        log.info("Removing node({}) from cluster({}).", nodeId, hostAndPort);

        // Load cluster information
        loadClusterInfoFromNode(hostAndPort);

        // Check if the node exists and is not empty
        final TribClusterNode node = getNodeByNodeId(nodeId);
        if (node == null) {
            throw new IllegalArgumentException(String.format("No such node ID %s", nodeId));
        }
        if (!node.getNode().getServedSlotsSet().isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Node(%s) is not empty! Reshard data away and try again.",
                                  node.getNode().getHostAndPort()));
        }

        // Send CLUSTER FORGET to all the nodes but the node to remove
        forgotNode(nodeId);

        if (StringUtils.equalsIgnoreCase(reset, "soft")) {
            log.info("SOFT RESET the node.");
            node.getRedisCommands().clusterReset(false);
        } else if (StringUtils.equalsIgnoreCase(reset, "hard")) {
            log.info("HARD RESET the node.");
            node.getRedisCommands().clusterReset(true);
        }

        if (shutdown) {
            log.info("SHUTDOWN the node.");
            node.getRedisCommands().shutdown(true);
        }
    }

    public void deleteFailNodeOfCluster(final String hostAndPort, final String nodeId)
            throws Exception {
        log.info("Removing node({}) from cluster({}).", nodeId, hostAndPort);

        // Load cluster information
        loadClusterInfoFromNode(hostAndPort);

        // Send CLUSTER FORGET to all the nodes but the node to remove
        forgotNode(nodeId);
    }

    void forgotNode(final String nodeId) {
        log.info("Sending CLUSTER FORGET messages to the cluster...");
        nodes.stream()
             .filter(v -> !StringUtils.equalsIgnoreCase(v.getNode().getNodeId(), nodeId))
             .forEach(v -> {
                 if (StringUtils.isNotBlank(v.getNode().getMasterNodeId())
                     && StringUtils.equalsIgnoreCase(v.getNode().getMasterNodeId(), nodeId)) {
                     // Reconfigure the slave to replicate with some other node
                     TribClusterNode master = getMasterWithLeastReplicasSpecifiedNodeIdExcluded(nodeId);
                     log.info("new master={}, old master = {}", master.getNode().getNodeId(), nodeId);
                     log.info("{} as replica of {}", v.getNode().getNodeId(), master.getNode().getNodeId());
                     v.getRedisCommands().clusterReplicate(master.getNode().getNodeId());
                 }
                 v.getRedisCommands().clusterForget(nodeId);
             });
    }

    public void replicateNode(final String hostAndPort, final String masterNodeId)
            throws Exception {
        log.info("Replicate node({}) as slave of {}.", hostAndPort, masterNodeId);

        loadClusterInfoFromNode(hostAndPort);

        final TribClusterNode node = getNodeByHostAndPort(hostAndPort);
        if (node == null) {
            throw new IllegalArgumentException(String.format("Node(%s) does not exists.", hostAndPort));
        }
        if (node.hasFlag("slave")) {
            // OK
        } else {
            if (!node.getNode().getServedSlotsSet().isEmpty()) {
                throw new IllegalArgumentException(
                        String.format("Node(%s) is not empty! Reshard data away and try again.", hostAndPort));
            }
        }

        final TribClusterNode master = getNodeByNodeId(masterNodeId);
        if (master == null) {
            throw new IllegalArgumentException(String.format("Node(%s) does not exists.", masterNodeId));
        }
        if (!master.hasFlag("master")) {
            throw new IllegalArgumentException(String.format("Node(%s) is not master.", masterNodeId));
        }

        log.info("Configure node({}) as replica of {}.", hostAndPort, masterNodeId);
        node.getRedisCommands().clusterReplicate(masterNodeId);
    }

    public void failoverNode(final String hostAndPort)
            throws Exception {
        log.info("Failover node({}).", hostAndPort);

        loadClusterInfoFromNode(hostAndPort);

        final TribClusterNode node = getNodeByHostAndPort(hostAndPort);
        if (node == null) {
            throw new IllegalArgumentException(String.format("Node(%s) does not exists.", hostAndPort));
        }
        if (node.hasFlag("slave")) {
            // OK
        } else {
            throw new IllegalArgumentException(String.format("Node(%s) is not slave.", hostAndPort));
        }

        node.getRedisCommands().clusterFailover(false);
    }

    void validateClusterAndEmptyNode(final TribClusterNode newNode) {
        newNode.connect(true);
        newNode.assertCluster();
        newNode.loadInfo();
        newNode.assertEmpty();
    }

    TribClusterNode getMasterWithLeastReplicas() {
        final List<TribClusterNode> masters = nodes.stream()
                                                   .filter(node -> node.hasFlag("master"))
                                                   .collect(Collectors.toList());
        masters.sort(Comparator.comparingInt(v -> v.getNode().getServedSlotsSet().size()));
        return masters.get(0);
    }

    TribClusterNode getMasterWithLeastReplicasSpecifiedNodeIdExcluded(final String nodeId) {
        final List<TribClusterNode> masters =
                nodes.stream()
                     .filter(node -> node.hasFlag("master"))
                     .filter(node -> !StringUtils.equalsIgnoreCase(node.getNode().getNodeId(), nodeId))
                     .collect(Collectors.toList());
        masters.sort(Comparator.comparingInt(v -> v.getNode().getServedSlotsSet().size()));
        return masters.get(0);
    }

    // Given a list of source nodes return a "resharding plan"
    // with what slots to move in order to move "numslots" slots to another
    // instance.
    List<ReshardTable> computeReshardTable(final List<TribClusterNode> sources, final int slotCount) {
        final List<ReshardTable> moved = Lists.newArrayList();

        // Sort from bigger to smaller instance, for two reasons:
        // 1) If we take less slots than instances it is better to start
        //    getting from the biggest instances.
        // 2) We take one slot more from the first instance in the case of not
        //    perfect divisibility. Like we have 3 nodes and need to get 10
        //    slots, we take 4 from the first, and 3 from the rest. So the
        //    biggest is always the first.
        sources.sort((o1, o2) -> Integer.compare(o2.getNode().getServedSlotsSet().size(),
                                                 o1.getNode().getServedSlotsSet().size()));
        final int sourceTotalSlot = sources.stream().mapToInt(
                source -> source.getNode().getServedSlotsSet().size()).sum();
        if (sourceTotalSlot < slotCount) {
            throw new IllegalArgumentException(String.format(
                    "Total slot count which is sum of from nodes is not enough. Slot count is %s, But total slot count is %s.",
                    slotCount, sourceTotalSlot));
        }

        int i = 0;
        for (final TribClusterNode source : sources) {
            // Every node will provide a number of slots proportional to the
            // slots it has assigned.
            double n = (double) slotCount / (double) sourceTotalSlot
                       * (double) source.getNode().getServedSlotsSet().size();
            if (i == 0) {
                n = Math.ceil(n);
            } else {
                n = Math.floor(n);
            }

            int j = 0;
            for (final Integer slot : source.getNode().getServedSlotsSet()) {
                if (j >= n || moved.size() >= slotCount) {
                    break;
                }
                moved.add(new ReshardTable(source, slot));
                j++;
            }

            i++;
        }

        return moved;
    }

    void clusterError(final String error) {
        log.warn(error);
        errors.add(error);
    }

    List<String> checkCluster() {
        // init error
        errors.clear();

        checkConfigConsistency();
        checkOpenSlots();
        checkSlotsCoverage();

        return errors;
    }

    void checkConfigConsistency() {
        if (isConfigConsistent()) {
            log.info("OK. All nodes agree about slots configuration.");
        } else {
            clusterError("Nodes don't agree about configuration!");
        }
    }

    boolean isConfigConsistent() {
        final Set<String> signatures = Sets.newHashSet();
        nodes.forEach(node -> signatures.add(node.getConfigSignature()));
        return signatures.size() == 1;
    }

    void checkOpenSlots() {
        log.info("Check for open slots.");
        final Set<Integer> openSlots = Sets.newHashSet();
        for (final TribClusterNode node : nodes) {
            if (!node.getNode().getMigrating().isEmpty()) {
                clusterError(String.format("[Warning] Node %s has slots in migrating state. (%s).",
                                           node.getNode().getHostAndPort(),
                                           StringUtils.join(node.getNode().getMigrating().keySet(), ",")));
                openSlots.addAll(node.getNode().getMigrating().keySet());
            }
            if (!node.getNode().getImporting().isEmpty()) {
                clusterError(String.format("[Warning] Node %s has slots in importing state (%s).",
                                           node.getNode().getHostAndPort(),
                                           StringUtils.join(node.getNode().getImporting().keySet(), ",")));
                openSlots.addAll(node.getNode().getImporting().keySet());
            }
        }
        if (!openSlots.isEmpty()) {
            log.warn("The following slots are open: {}", openSlots);
        }
        if (fix) {
            openSlots.forEach(this::fixOpenSlot);
        }
    }

    // Slot 'slot' was found to be in importing or migrating state in one or
    // more nodes. This function fixes this condition by migrating keys where
    // it seems more sensible.
    void fixOpenSlot(final int slot) {
        log.info("Fixing open slots. {}", slot);

        // Try to obtain the current slot owner, according to the current
        // nodes configuration.
        final List<TribClusterNode> owners = getSlotOwners(slot);
        TribClusterNode owner = owners.size() == 1 ? owners.get(0) : null;

        final List<TribClusterNode> migrating = Lists.newArrayList();
        final List<TribClusterNode> importing = Lists.newArrayList();
        for (final TribClusterNode node : nodes) {
            if (node.hasFlag("slave")) {
                continue;
            }
            if (node.getNode().getMigrating().containsKey(slot)) {
                migrating.add(node);
            } else if (node.getNode().getImporting().containsKey(slot)) {
                importing.add(node);
            } else if (node.getRedisCommands().clusterCountKeysInSlot(slot) > 0 && !node.equals(owner)) {
                log.info("Found keys about slot {} in node {}", slot, node);
                importing.add(node);
            }
        }
        log.info("Set as migrating in: {}", migrating);
        log.info("Set as migrating in: {}", importing);

        // If there is no slot owner, set as owner the slot with the biggest
        // number of keys, among the set of migrating / importing nodes.
        if (owner == null) {
            log.info("Nobody claims ownership, selecting an owner...");
            owner = getNodeWithMostKeysInSlot(nodes, slot);

            // If we still don't have an owner, we can't fix it.
            if (owner == null) {
                throw new RedisTribException("Can't select a slot owner. Impossible to fix.");
            }

            // Use ADDSLOTS to assign the slot.
            log.info("Configuring {} as the slot owner", owner);
            owner.getRedisCommands().clusterSetSlotStable(slot);
            owner.getRedisCommands().clusterAddSlots(slot);

            // Make sure this information will propagate. Not strictly needed
            // since there is no past owner, so all the other nodes will accept
            // whatever epoch this node will claim the slot with.
            owner.getRedisCommands().clusterBumpepoch();

            // Remove the owner from the list of migrating/importing
            // nodes.
            migrating.remove(owner);
            importing.remove(owner);
        }

        // If there are multiple owners of the slot, we need to fix it
        // so that a single node is the owner and all the other nodes
        // are in importing state. Later the fix can be handled by one
        // of the base cases above.
        //
        // Note that this case also covers multiple nodes having the slot
        // in migrating state, since migrating is a valid state only for
        // slot owners.
        if (owners.size() > 1) {
            for (final TribClusterNode node : owners) {
                if (node.equals(owner)) {
                    continue;
                }
                node.getRedisCommands().clusterDelSlots(slot);
                node.getRedisCommands().clusterSetSlotImporting(slot, owner.getNode().getNodeId());

                importing.remove(node); // Avoid duplicates
                importing.add(node);
            }
            owner.getRedisCommands().clusterBumpepoch();
        }

        if (migrating.size() == 1 && importing.size() == 1) {
            // Case 1: The slot is in migrating state in one slot, and in
            // importing state in 1 slot. That's trivial to address.
            moveSlot(migrating.get(0), importing.get(0), slot, Sets.newHashSet("fix"));
        } else if (migrating.isEmpty() && !importing.isEmpty()) {
            // Case 2: There are multiple nodes that claim the slot as importing,
            // they probably got keys about the slot after a restart so opened
            // the slot. In this case we just move all the keys to the owner
            // according to the configuration.
            log.info("Moving all the {} slot keys to its owner {}", slot, owner);
            for (final TribClusterNode importingNode : importing) {
                if (importingNode.equals(owner)) {
                    continue;
                }
                moveSlot(importingNode, owner, slot, Sets.newHashSet("fix", "cold"));
                log.info("Setting {} as STABLE in {}", slot, importingNode);
                importingNode.getRedisCommands().clusterSetSlotStable(slot);
            }
        } else if (importing.isEmpty() && migrating.size() == 1
                   && migrating.get(0).getRedisCommands().clusterGetKeysInSlot(slot, 10).isEmpty()) {
            // Case 3: There are no slots claiming to be in importing state, but
            // there is a migrating node that actually don't have any key. We
            // can just close the slot, probably a reshard interrupted in the middle.
            migrating.get(0).getRedisCommands().clusterSetSlotStable(slot);
        } else {
            throw new RedisTribException(String.format(
                    "Sorry, can't fix this slot yet (work in progress)."
                    + "Slot is set as migrating in %s, as importing in %s, owner is %s",
                    StringUtils.join(migrating, ","),
                    StringUtils.join(importing, ","),
                    owner
            ));
        }
    }

    // Move slots between source and target nodes using MIGRATE.
    //
    // Options:
    //     :fix     -- We are moving in the context of a fix. Use REPLACE.
    //     :cold    -- Move keys without opening slots / reconfiguring the nodes.
    //     :update  -- Update nodes.info[:slots] for source/target nodes.
    void moveSlot(final TribClusterNode source, final TribClusterNode target, final int slot,
                  Set<String> options) {
        if (options == null) {
            options = Sets.newHashSet();
        }

        // We start marking the slot as importing in the destination node,
        // and the slot as migrating in the target host. Note that the order of
        // the operations is important, as otherwise a client may be redirected
        // to the target node that does not yet know it is importing this slot.
        log.info("Moving slot {} from {} to {}.", slot, source, target);

        if (!options.contains("cold")) {
            target.getRedisCommands().clusterSetSlotImporting(slot, source.getNode().getNodeId());
            source.getRedisCommands().clusterSetSlotMigrating(slot, target.getNode().getNodeId());
        }

        // Migrate all the keys from source to target using the MIGRATE command
        while (true) {
            final List<String> keys = source.getRedisCommands().clusterGetKeysInSlot(slot, 100);
            if (keys.isEmpty()) {
                break;
            }

            try {
                source.getRedisCommands().migrate(
                        target.getNode().getHost(), target.getNode().getPort(),
                        0, 60000, MigrateArgs.Builder.keys(keys));
            } catch (Exception e) {
                if (options.contains("fix") && StringUtils.contains(e.getMessage(), "BUSYKEY")) {
                    log.info("Target key exists. Replacing it for FIX.");
                    source.getRedisCommands().migrate(
                            target.getNode().getHost(), target.getNode().getPort(),
                            0, 60000, MigrateArgs.Builder.keys(keys).replace());
                } else {
                    throw new RedisTribException("Calling MIGRATE: " + e.getMessage());
                }
            }
        }

        // Set the new node as the owner of the slot in all the known nodes.
        if (!options.contains("cold")) {
            nodes.stream()
                 .filter(node -> !node.hasFlag("slave"))
                 .forEach(node -> node.getRedisCommands()
                                      .clusterSetSlotNode(slot, target.getNode().getNodeId()));
        }

        // Update the node logical config
        if (options.contains("update")) {
            source.getTmpSlots().remove(slot);
            target.getNode().getServedSlotsSet().add(slot);
        }
    }

    List<TribClusterNode> getSlotOwners(final int slot) {
        return nodes.stream()
                    .filter(node -> !node.hasFlag("slave"))
                    .filter(node -> node.getNode().getServedSlotsSet().contains(slot))
                    .collect(Collectors.toList());
    }

    // Return the node, among 'nodes' with the greatest number of keys
    // in the specified slot.
    TribClusterNode getNodeWithMostKeysInSlot(final List<TribClusterNode> nodes, final int slot) {
        TribClusterNode best = null;
        long bestNumKeys = 0;

        for (final TribClusterNode node : nodes) {
            if (node.hasFlag("slave")) {
                continue;
            }
            final long numKeys = node.getRedisCommands().clusterCountKeysInSlot(slot);
            if (numKeys > bestNumKeys || best == null) {
                best = node;
                bestNumKeys = numKeys;
            }
        }

        return best;
    }

    List<TribClusterNode> getNodesWithKeysInSlot(final int slot) {
        return nodes.stream()
                    .filter(node -> !node.hasFlag("slave"))
                    .filter(node -> !node.getRedisCommands().clusterGetKeysInSlot(slot, 1).isEmpty())
                    .collect(Collectors.toList());
    }

    void checkSlotsCoverage() {
        log.info("Check slots coverage.");
        final Set<Integer> slots = coveredSlots();
        if (slots.size() == RedisSupport.ALL_SLOTS_SIZE) {
            log.info("All 16384 slots covered.");
        } else {
            log.warn("Not all 16384 slots covered.");
            clusterError("Not all 16384 slots covered by nodes.");
            if (fix) {
                fixSlotsCoverage();
            }
        }
    }

    Set<Integer> coveredSlots() {
        final Set<Integer> slots = Sets.newTreeSet();
        nodes.forEach(node -> slots.addAll(node.getNode().getServedSlotsSet()));
        return slots;
    }

    void fixSlotsCoverage() {
        final Set<Integer> notCovered = Sets.newTreeSet();
        for (int i = 0; i < RedisSupport.ALL_SLOTS_SIZE; i++) {
            notCovered.add(i);
        }
        notCovered.removeAll(coveredSlots());

        log.info("Fixing slots coverage...");
        log.info("List of not covered slots: {}", notCovered);

        // For every slot, take action depending on the actual condition:
        // 1) No node has keys for this slot.
        // 2) A single node has keys for this slot.
        // 3) Multiple nodes have keys for this slot.
        final Map<Integer, List<TribClusterNode>> slots = Maps.newLinkedHashMap();
        notCovered.forEach(slot -> {
            final List<TribClusterNode> nodesWithKeysInSlot = getNodesWithKeysInSlot(slot);
            slots.put(slot, nodesWithKeysInSlot);
            log.info("Slot {} has keys in {} nodes: {}", slot, nodesWithKeysInSlot.size(), nodesWithKeysInSlot);
        });

        final Map<Integer, List<TribClusterNode>> none = slots.entrySet()
                                                              .stream()
                                                              .filter(e -> e.getValue().isEmpty())
                                                              .collect(Collectors.toMap(Entry::getKey,
                                                                                        Entry::getValue));
        final Map<Integer, List<TribClusterNode>> single = slots.entrySet()
                                                                .stream()
                                                                .filter(e -> e.getValue().size() == 1)
                                                                .collect(Collectors.toMap(Entry::getKey,
                                                                                          Entry::getValue));
        final Map<Integer, List<TribClusterNode>> multi = slots.entrySet()
                                                               .stream()
                                                               .filter(e -> e.getValue().size() > 1)
                                                               .collect(Collectors.toMap(Entry::getKey,
                                                                                         Entry::getValue));

        // Handle case "1": keys in no node.
        if (!none.isEmpty()) {
            log.info("The following uncovered slots have no keys across the cluster: {}", none.keySet());
            none.forEach((slot, noneNodes) -> {
                List<TribClusterNode> masters = nodes.stream()
                                                     .filter(v -> v.hasFlag("master"))
                                                     .collect(Collectors.toList());
                final TribClusterNode node = masters.get(new Random().nextInt(masters.size()));
                log.info("Covering slot {} with {}", slot, node);
                node.getRedisCommands().clusterAddSlots(slot);
            });
        }

        // Handle case "2": keys only in one node.
        if (!single.isEmpty()) {
            log.info("The following uncovered slots have keys in just one node: {}", single.keySet());
            single.forEach((slot, singleNodes) -> {
                log.info("Covering slot {} with {}", slot, singleNodes.get(0));
                singleNodes.get(0).getRedisCommands().clusterAddSlots(slot);
            });
        }

        // Handle case "3": keys in multiple nodes.
        if (!multi.isEmpty()) {
            log.info("The following uncovered slots have keys in multiple nodes: {}", multi.keySet());
            multi.forEach((slot, multiNodes) -> {
                final TribClusterNode target = getNodeWithMostKeysInSlot(multiNodes, slot);
                log.info("Covering slot {} moving keys to {}", slot, target);

                target.getRedisCommands().clusterAddSlots(slot);
                target.getRedisCommands().clusterSetSlotStable(slot);
                multiNodes.stream().filter(src -> !src.equals(target)).forEach(src -> {
                    // Set the source node in 'importing' state (even if we will
                    // actually migrate keys away) in order to avoid receiving
                    // redirections for MIGRATE.
                    src.getRedisCommands().clusterSetSlotImporting(slot, target.getNode().getNodeId());
                    moveSlot(src, target, slot, Sets.newHashSet("fix", "cold"));
                    src.getRedisCommands().clusterSetSlotStable(slot);
                });
            });
        }
    }

    void flushNodesConfig() {
        nodes.forEach(TribClusterNode::flushNodeConfig);
    }

    // Redis Cluster config epoch collision resolution code is able to eventually
    // set a different epoch to each node after a new cluster is created, but
    // it is slow compared to assign a progressive config epoch to each node
    // before joining the cluster. However we do just a best-effort try here
    // since if we fail is not a problem.
    void assignConfigEpoch() {
        int configEpoch = 1;
        for (final TribClusterNode node : nodes) {
            try {
                node.getRedisCommands().clusterSetConfigEpoch(configEpoch);
            } catch (Exception ignored) {
            }
            configEpoch++;
        }
    }

    // We use a brute force approach to make sure the node will meet
    // each other, that is, sending CLUSTER MEET messages to all the nodes
    // about the very same node.
    // Thanks to gossip this information should propagate across all the
    // cluster in a matter of seconds.
    void joinCluster() {
        TribClusterNode firstNode = null;
        for (final TribClusterNode node : nodes) {
            if (firstNode == null) {
                firstNode = node;
            } else {
                node.getRedisCommands().clusterMeet(firstNode.getNode().getHost(),
                                                    firstNode.getNode().getPort());
            }
        }
    }

    void waitClusterJoin() {
        log.info("Waiting for the cluster join.");
        final StringBuilder waiting = new StringBuilder(".");
        while (!isConfigConsistent()) {
            log.info(waiting.toString());
            waiting.append('.');
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    void checkCreateParameters() {
        final int masters = nodes.size() / (replicas + 1);
        if (masters < 3) {
            final String errorMessage = String.format(
                    "Redis Cluster requires at least 3 master nodes. " +
                    "This is not possible with %s nodes and %s replicas per node. " +
                    "At least %s nodes are required.",
                    nodes.size(), replicas, 3 * (replicas + 1)
            );
            throw new IllegalArgumentException(errorMessage);
        }
    }

    void addNodes(final TribClusterNode node) {
        nodes.add(node);
    }

    void allocSlots() {
        final int mastersCount = nodes.size() / (replicas + 1);
        final List<TribClusterNode> masters;

        // The first step is to split instances by IP. This is useful as
        // we'll try to allocate master nodes in different physical machines
        // (as much as possible) and to allocate slaves of a given master in
        // different physical machines as well.
        //
        // This code assumes just that if the IP is different, than it is more
        // likely that the instance is running in a different physical host
        // or at least a different virtual machine.
        final Map<String, List<TribClusterNode>> ips = Maps.newTreeMap();
        ips.putAll(nodes.stream()
                        .collect(Collectors.groupingBy(v -> v.getNode().getHost(), Collectors.toList())));
        // sort hostNodes
        for (final Map.Entry<String, List<TribClusterNode>> e : ips.entrySet()) {
            final List<TribClusterNode> hostNodes = e.getValue();
            hostNodes.sort((o1, o2) -> {
                if (StringUtils.equals(o1.getNode().getHost(), o2.getNode().getHost())) {
                    return Integer.compare(o1.getNode().getPort(), o2.getNode().getPort());
                } else {
                    return o1.getNode().getHost().compareTo(o2.getNode().getHost());
                }
            });
        }

        log.info("Select master instances.");
        List<TribClusterNode> interleaved = Lists.newArrayList();
        boolean stop = false;
        while (!stop) {
            // Take one node from each IP until we run out of nodes
            // across every IP.
            for (final Map.Entry<String, List<TribClusterNode>> e : ips.entrySet()) {
                final List<TribClusterNode> hostNodes = e.getValue();

                if (hostNodes.isEmpty()) {
                    // if this IP has no remaining nodes, check for termination
                    if (interleaved.size() == nodes.size()) {
                        // stop when 'interleaved' has accumulated all nodes
                        stop = true;
                        continue;
                    }
                } else {
                    // else, move one node from this IP to 'interleaved'
                    interleaved.add(hostNodes.remove(0));
                }
            }
        }

        masters = Lists.newArrayList(interleaved.subList(0, mastersCount));
        interleaved = Lists.newArrayList(interleaved.subList(mastersCount, interleaved.size()));
        log.info("masters={}", masters);
        log.info("interleaved={}", interleaved);

        log.info("Alloc slots on masters.");
        final double slotsPerNode = RedisSupport.ALL_SLOTS_SIZE / mastersCount;
        int first = 0;
        int last;
        double cursor = 0.0;
        for (int i = 0; i < masters.size(); i++) {
            last = (int) Math.round(cursor + slotsPerNode - 1.0);
            if (last > RedisSupport.ALL_SLOTS_SIZE || i == masters.size() - 1) {
                last = RedisSupport.ALL_SLOTS_SIZE - 1;
            }
            if (last < first) { // Min step is 1
                last = first;
            }

            final Set<Integer> slots = Sets.newTreeSet(
                    IntStream.rangeClosed(first, last).boxed().collect(Collectors.toList()));
            log.info("Add slots to {}. first={}, last={}", masters.get(i), first, last);
            masters.get(i).addTmpSlots(slots);

            first = last + 1;
            cursor += slotsPerNode;
        }

        // Select N replicas for every master.
        // We try to split the replicas among all the IPs with spare nodes
        // trying to avoid the host where the master is running, if possible.
        //
        // Note we loop two times.  The first loop assigns the requested
        // number of replicas to each master.  The second loop assigns any
        // remaining instances as extra replicas to masters.  Some masters
        // may end up with more than their requested number of replicas, but
        // all nodes will be used.
        log.info("Select requested replica.");
        final Map<TribClusterNode, List<TribClusterNode>> replicasOfMaster = Maps.newLinkedHashMap();
        for (final TribClusterNode master : masters) {
            replicasOfMaster.put(master, Lists.newArrayList());
            while (replicasOfMaster.get(master).size() < replicas) {
                if (interleaved.isEmpty()) {
                    log.info("break because node count is 0");
                    break;
                }

                // Return the first node not matching our current master
                final Optional<TribClusterNode> replicaNodeOp =
                        interleaved.stream()
                                   .filter(v -> {
                                       boolean isNotSameHostAsMaster = !StringUtils.equals(
                                               master.getNode().getHost(), v.getNode().getHost());
                                       boolean isSameHostAsAlreadyRegisteredReplica =
                                               replicasOfMaster.get(master)
                                                               .stream()
                                                               .anyMatch(
                                                                       rpl -> StringUtils.equals(
                                                                               v.getNode().getHost(),
                                                                               rpl.getNode().getHost()));
                                       return isNotSameHostAsMaster
                                              && !isSameHostAsAlreadyRegisteredReplica;
                                   }).findFirst();

                final TribClusterNode replicaNode;
                if (replicaNodeOp.isPresent()) {
                    replicaNode = replicaNodeOp.get();
                    interleaved.remove(replicaNode);
                } else {
                    replicaNode = interleaved.remove(0);
                }

                log.info("Select {} as replica of {}", replicaNode, master);
                replicaNode.setAsReplica(master.getNode().getNodeId());
                replicasOfMaster.get(master).add(replicaNode);
            }
        }

        // we want to attach different host of master and min size.
        log.info("Select extra replica.");
        for (final TribClusterNode extra : interleaved) {
            Entry<TribClusterNode, List<TribClusterNode>> entrySetNode =
                    replicasOfMaster.entrySet()
                                    .stream()
                                    .min(Comparator.comparingInt(v -> v.getValue().size()))
                                    .orElseThrow(IllegalStateException::new);

            final int minSize = entrySetNode.getValue().size();
            final int sameMinCount = (int) replicasOfMaster.entrySet().stream()
                                                           .filter(v -> minSize == v.getValue().size())
                                                           .count();
            final int sameHostOfMasterCount =
                    (int) masters.stream()
                                 .filter(v -> StringUtils.equals(v.getNode().getHost(),
                                                                 extra.getNode().getHost()))
                                 .count();

            if (sameMinCount > sameHostOfMasterCount) {
                entrySetNode =
                        replicasOfMaster.entrySet()
                                        .stream()
                                        .filter(e -> !StringUtils.equals(e.getKey().getNode().getHost(),
                                                                         extra.getNode().getHost()))
                                        .min(Comparator.comparingInt(v -> v.getValue().size()))
                                        .orElseThrow(IllegalStateException::new);
            }

            final TribClusterNode master = entrySetNode.getKey();
            log.info("Select {} as replica of {} (extra)", extra, master);
            extra.setAsReplica(master.getNode().getNodeId());
            replicasOfMaster.get(master).add(extra);
        }
    }

    List<CreateClusterParam> buildCreateClusterParam() {
        // first loop (master)
        final List<CreateClusterParam> params = Lists.newArrayList();
        nodes.stream()
             // is master
             .filter(node -> StringUtils.isBlank(node.getNode().getMasterNodeId()))
             .forEach(node -> {
                 final int startSlotNumber = node.getTmpSlots().stream().min(Integer::compare).get();
                 final int endSlotNumber = node.getTmpSlots().stream().max(Integer::compare).get();

                 final CreateClusterParam param = new CreateClusterParam();
                 param.setStartSlotNumber(String.valueOf(startSlotNumber));
                 param.setEndSlotNumber(String.valueOf(endSlotNumber));
                 param.setMaster(node.getNode().getHostAndPort());
                 param.setMasterNodeId(node.getNode().getNodeId());
                 params.add(param);
             });

        // replica loop
        nodes.stream()
             .filter(node -> StringUtils.isNotBlank(node.getNode().getMasterNodeId()))
             // is slave
             .forEach(node -> {
                 final String masterNodeId = node.getNode().getMasterNodeId();
                 params.stream()
                       .filter(param -> StringUtils.equals(param.getMasterNodeId(), masterNodeId))
                       .forEach(param -> param.getReplicas().add(node.getNode().getHostAndPort()));
             });

        // sort
        params.sort(Comparator.comparingInt(v -> Integer.valueOf(v.getStartSlotNumber())));

        return params;
    }

    void loadClusterInfoFromNode(final String hostAndPort) {
        final TribClusterNode node = createTribClusterNode(hostAndPort);
        node.connect(true);
        node.assertCluster();
        node.loadInfo(true);
        addNodes(node);

        for (final ClusterNode friend : node.getFriends()) {
            if (friend.hasFlag("noaddr") || friend.hasFlag("disconnected") || friend.hasFlag("fail")) {
                continue;
            }
            final TribClusterNode friendTribNode = createTribClusterNode(friend.getHostAndPort());
            friendTribNode.connect();
            if (friendTribNode.getRedisCommands() == null) {
                continue;
            }
            try {
                friendTribNode.loadInfo();
                addNodes(friendTribNode);
            } catch (Exception ignored) {
                log.error("Unable to load info for node {}.", friend.getHostAndPort());
            }
        }

        populateNodesReplicasInfo();
    }

    // This function is called by load_cluster_info_from_node in order to
    // add additional information to every node as a list of replicas.
    void populateNodesReplicasInfo() {
        // Populate the replicas field using the replicate field of slave
        // nodes.
        nodes.forEach(node -> {
            final String masterNodeId = node.getNode().getMasterNodeId();
            if (StringUtils.isNotBlank(masterNodeId)) {
                final TribClusterNode master = getNodeByNodeId(masterNodeId);
                log.info("master={}, masterNodeId={}", master, masterNodeId);
                if (master == null) {
                    log.warn("{} claims to be slave of unknown node ID {}.", node, masterNodeId);
                } else {
                    master.getReplicas().add(node);
                }
            }
        });
    }

    TribClusterNode getNodeByNodeId(final String nodeId) {
        return nodes.stream()
                    .filter(node -> StringUtils.equalsIgnoreCase(node.getNode().getNodeId(), nodeId))
                    .findFirst()
                    .orElse(null);
    }

    TribClusterNode getNodeByHostAndPort(final String hostAndPort) {
        return nodes.stream()
                    .filter(node -> StringUtils.equalsIgnoreCase(node.getNode().getHostAndPort(), hostAndPort))
                    .findFirst()
                    .orElse(null);
    }

    TribClusterNode createTribClusterNode(final String hostAndPort) {
        return new TribClusterNode(hostAndPort, "");
    }

    @Override
    public void close() throws IOException {
        if (nodes != null) {
            nodes.forEach(v -> {
                try {
                    v.close();
                } catch (Exception ignored) {
                    log.error("Failed to close redis connection.");
                }
            });
        }
    }

    @Data
    public static class ReshardTable {
        private TribClusterNode source;
        private int slot;

        public ReshardTable(TribClusterNode source, int slot) {
            this.source = source;
            this.slot = slot;
        }
    }

    public static class RedisTribException extends RuntimeException {
        private static final long serialVersionUID = -2165202927348232067L;

        public RedisTribException(String message) {
            super(message);
        }
    }
}
