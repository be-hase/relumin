package com.behase.relumin.support;

import com.behase.relumin.Constants;
import com.behase.relumin.exception.ApiException;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.util.ValidationUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import redis.clients.jedis.JedisCluster.Reset;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Almost clone of redis-trib.rb
 *
 * @author Ryosuke Hasebe
 */
@Slf4j
public class RedisTrib implements Closeable {
    private int replicas;
    private boolean fix;

    @Getter
    private List<String> errors = Lists.newArrayList();

    @Getter
    private List<TribClusterNode> nodes = Lists.newArrayList();

    public List<CreateClusterParam> getCreateClusterParams(int replicas, Set<String> hostAndPorts) {
        ValidationUtils.replicas(replicas);

        this.replicas = replicas;

        for (String hostAndPort : hostAndPorts) {
            hostAndPort = StringUtils.trim(hostAndPort);
            TribClusterNode node = new TribClusterNode(hostAndPort);
            node.connect(true);
            node.assertCluster();
            node.loadInfo();
            node.assertEmpty();
            addNodes(node);
        }

        checkCreateParameters();
        allocSlots();

        return buildCreateClusterParam();
    }

    public void createCluster(List<CreateClusterParam> params) throws Exception {
        ValidationUtils.createClusterParams(params);

        log.info("Creating cluster.");
        // set nodes
        params.forEach(param -> {
            int startSlot = Integer.valueOf(param.getStartSlotNumber());
            int endSlot = Integer.valueOf(param.getEndSlotNumber());
            List<Integer> tmpSlots = Lists.newArrayList();
            for (int i = startSlot; i <= endSlot; i++) {
                tmpSlots.add(i);
            }

            TribClusterNode masterNode = new TribClusterNode(param.getMaster());
            masterNode.connect(true);
            masterNode.assertCluster();
            masterNode.loadInfo();
            masterNode.assertEmpty();
            addNodes(masterNode);

            param.getReplicas().forEach(replica -> {
                TribClusterNode replicaNode = new TribClusterNode(replica);
                replicaNode.connect(true);
                replicaNode.assertCluster();
                replicaNode.loadInfo();
                replicaNode.assertEmpty();
                replicaNode.setAsReplica(masterNode.getNodeInfo().getNodeId());
                addNodes(replicaNode);
            });

            masterNode.addTmpSlots(tmpSlots);
        });

        // flush nodes config
        flushNodesConfig();
        log.info("Nodes configuration updated.");
        log.info("Assign a different config epoch to each node.");

        // assign config epoch
        assignConfigEpoch();

        joinCluster();
        // Give one second for the join to start, in order to avoid that
        // wait_cluster_join will find all the nodes agree about the config as
        // they are still empty with unassigned slots.
        Thread.sleep(1000);
        waitClusterJoin();
        flushNodesConfig();

        checkCluster();

        // Give one 3 second for gossip
        Thread.sleep(3000);
    }

    public List<String> checkCluster(String hostAndPort) {
        loadClusterInfoFromNode(hostAndPort);
        return checkCluster();
    }

    public void fixCluster(String hostAndPort) throws Exception {
        fix = true;
        loadClusterInfoFromNode(hostAndPort);
        checkCluster();

        // Give one 3 second for gossip
        Thread.sleep(3000);
    }

    public void reshardCluster(String hostAndPort, int slotCount, String fromNodeIds, String toNodeId) throws Exception {
        ValidationUtils.slotCount(slotCount);
        ValidationUtils.notBlank(toNodeId, "toNodeId");
        ValidationUtils.notBlank(fromNodeIds, "fromNodeId");

        loadClusterInfoFromNode(hostAndPort);
        checkCluster();
        if (errors.size() > 0) {
            throw new ApiException(Constants.ERR_CODE_CLUSTER_HAS_ERRORS, "Please fix your cluster problems before resharding.", HttpStatus.BAD_REQUEST);
        }

        TribClusterNode target = getNodeByNodeId(toNodeId);
        if (target == null || target.hasFlag("slave")) {
            throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "The specified node is not known or is not a master, please retry.", HttpStatus.BAD_REQUEST);
        }

        List<TribClusterNode> sources = Lists.newArrayList();
        if (StringUtils.equals("ALL", fromNodeIds)) {
            for (TribClusterNode node : nodes) {
                if (StringUtils.equals(node.getNodeInfo().getNodeId(), target.getNodeInfo().getNodeId())) {
                    continue;
                }
                if (node.hasFlag("slave")) {
                    continue;
                }
                if (node.getNodeInfo().getServedSlotsSet().size() == 0) {
                    continue;
                }
                sources.add(node);
            }
        } else {
            for (String fromNodeId : StringUtils.split(fromNodeIds, ",")) {
                TribClusterNode fromNode = getNodeByNodeId(fromNodeId);
                if (fromNode == null || fromNode.hasFlag("slave")) {
                    throw new InvalidParameterException(String.format("The specified node(%s) is not known or is not a master, please retry.", fromNodeId));
                }
                if (fromNode.getNodeInfo().getServedSlotsSet().size() == 0) {
                    throw new InvalidParameterException(String.format("The specified node(%s) does not have slots, please retry.", fromNodeId));
                }
                sources.add(fromNode);
            }
        }

        if (sources.stream().filter(source -> {
            return StringUtils.equals(source.getNodeInfo().getNodeId(), target.getNodeInfo().getNodeId());
        }).findAny().isPresent()) {
            throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Target node is also listed among the source nodes!", HttpStatus.BAD_REQUEST);
        }

        log.info("Ready to move {} slots.", slotCount);
        log.info("Source nodes : {}", sources.stream().map(v -> String.format("%s (%s)", v.getNodeInfo().getHostAndPort(), v.getNodeInfo().getNodeId())).collect(Collectors.toList()));
        log.info("Destination nodes : {}", String.format("%s (%s)", target.getNodeInfo().getHostAndPort(), target.getNodeInfo().getNodeId()));
        List<ReshardTable> reshardTables = computeReshardTable(sources, slotCount);
        reshardTables.forEach(reshardTable -> {
            moveSlot(reshardTable.getSource(), target, reshardTable.getSlot(), false, false);
        });

        // Give one 3 second for gossip
        Thread.sleep(3000);
    }

    public void reshardClusterBySlots(String hostAndPort, Set<Integer> slots, String toNodeId) throws Exception {
        ValidationUtils.notBlank(toNodeId, "toNodeId");

        loadClusterInfoFromNode(hostAndPort);
        checkCluster();
        if (errors.size() > 0) {
            throw new ApiException(Constants.ERR_CODE_CLUSTER_HAS_ERRORS, "Please fix your cluster problems before resharding.", HttpStatus.BAD_REQUEST);
        }

        TribClusterNode target = getNodeByNodeId(toNodeId);
        if (target == null || target.hasFlag("slave")) {
            throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "The specified node is not known or is not a master, please retry.", HttpStatus.BAD_REQUEST);
        }

        List<ReshardTable> reshardTables = Lists.newArrayList();
        Set<Integer> notFoundSlot = Sets.newHashSet();
        for (Integer slot : slots) {
            boolean found = false;

            if (target.getNodeInfo().getServedSlotsSet().contains(slot)) {
                continue;
            }
            for (TribClusterNode node : nodes) {
                if (StringUtils.equals(node.getNodeInfo().getNodeId(), target.getNodeInfo().getNodeId())) {
                    continue;
                }
                if (node.hasFlag("slave")) {
                    continue;
                }
                if (node.getNodeInfo().getServedSlotsSet().size() == 0) {
                    continue;
                }
                if (node.getNodeInfo().getServedSlotsSet().contains(slot)) {
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
            throw new InvalidParameterException(String.format("Cannot find the nodes which has slots(%s).", new JedisSupport().slotsDisplay(notFoundSlot)));
        }

        reshardTables.forEach(reshardTable -> {
            moveSlot(reshardTable.getSource(), target, reshardTable.getSlot(), false, false);
        });

        // Give one 3 second for gossip
        Thread.sleep(3000);
    }

    public void addNodeIntoCluster(final String hostAndPort, final String newHostAndPort) throws Exception {
        ValidationUtils.hostAndPort(newHostAndPort);

        log.info("Adding node {} to cluster {}", newHostAndPort, hostAndPort);
        loadClusterInfoFromNode(hostAndPort);
        checkCluster();

        // Add the new node
        TribClusterNode newNode = new TribClusterNode(newHostAndPort);
        newNode.connect(true);
        newNode.assertCluster();
        newNode.loadInfo();
        newNode.assertEmpty();
        addNodes(newNode);

        // Send CLUSTER MEET command to the new node
        TribClusterNode node = getNodeByHostAndPort(hostAndPort);
        log.info("Send CLUSTER MEET to node {} to make it join the cluster.", newHostAndPort);
        newNode.getJedis().clusterMeet(node.getNodeInfo().getHost(), node.getNodeInfo().getPort());

        log.info("New node added correctly.");

        // Give one 3 second for gossip
        Thread.sleep(3000);
    }

    public void addNodeIntoClusterAsReplica(final String hostAndPort, final String newHostAndPort,
                                            final String materNodeId)
            throws Exception {
        ValidationUtils.hostAndPort(newHostAndPort);

        log.info("Adding node {} to cluster {}", newHostAndPort, hostAndPort);
        loadClusterInfoFromNode(hostAndPort);
        checkCluster();

        // If --master-id was specified, try to resolve it now so that we
        // abort before starting with the node configuration.
        TribClusterNode master;
        if (StringUtils.equals(materNodeId, "RANDOM")) {
            master = getMasterWithLeastReplicas();
            log.info("Automatically selected master {} ().", master.getNodeInfo().getHostAndPort(), master.getNodeInfo().getMasterNodeId());
        } else {
            master = getNodeByNodeId(materNodeId);
            if (master == null) {
                throw new InvalidParameterException(String.format("No such master ID %s", materNodeId));
            }
        }

        // Add the new node
        TribClusterNode newNode = new TribClusterNode(newHostAndPort);
        newNode.connect(true);
        newNode.assertCluster();
        newNode.loadInfo();
        newNode.assertEmpty();
        addNodes(newNode);

        // Send CLUSTER MEET command to the new node
        TribClusterNode node = getNodeByHostAndPort(hostAndPort);
        log.info("Send CLUSTER MEET to node {} to make it join the cluster.", newHostAndPort);
        newNode.getJedis().clusterMeet(node.getNodeInfo().getHost(), node.getNodeInfo().getPort());

        // Additional configuration is needed if the node is added as
        // a slave.
        waitClusterJoin();
        log.info("Configure node as replica of {}.", master.getNodeInfo().getNodeId());
        newNode.getJedis().clusterReplicate(master.getNodeInfo().getNodeId());

        log.info("New node added correctly.");

        // Give one 3 second for gossip
        Thread.sleep(3000);
    }

    public void deleteNodeOfCluster(final String hostAndPort, String nodeId, String reset, boolean shutdown)
            throws Exception {
        ValidationUtils.notBlank(nodeId, "nodeId");

        log.info("Removing node({}) from cluster({}).", nodeId, hostAndPort);

        // Load cluster information
        loadClusterInfoFromNode(hostAndPort);

        // Check if the node exists and is not empty
        TribClusterNode node = getNodeByNodeId(nodeId);
        if (node == null) {
            throw new InvalidParameterException(String.format("No such node ID %s", nodeId));
        }
        if (node.getNodeInfo().getServedSlotsSet().size() > 0) {
            throw new InvalidParameterException(String.format("Node(%s) is not empty! Reshard data away and try again.", node.getNodeInfo().getHostAndPort()));
        }

        // Send CLUSTER FORGET to all the nodes but the node to remove
        log.info("Sending CLUSTER FORGET messages to the cluster...");
        nodes.stream().filter(v -> {
            return !StringUtils.equalsIgnoreCase(v.getNodeInfo().getNodeId(), nodeId);
        }).forEach(v -> {
            if (StringUtils.isNotBlank(v.getNodeInfo().getMasterNodeId())
                    && StringUtils.equalsIgnoreCase(v.getNodeInfo().getMasterNodeId(), nodeId)) {
                // Reconfigure the slave to replicate with some other node
                TribClusterNode master = getMasterWithLeastReplicasSpecifiedNodeIdExcluded(nodeId);
                log.info("new master={}, old master = {}", master.getNodeInfo().getNodeId(), nodeId);
                log.info("{} as replica of {}", v.getNodeInfo().getNodeId(), master.getNodeInfo().getNodeId());
                v.getJedis().clusterReplicate(master.getNodeInfo().getNodeId());
            }
            v.getJedis().clusterForget(nodeId);
        });

        if (StringUtils.isNoneBlank(reset)) {
            if (StringUtils.equalsIgnoreCase(reset, "soft")) {
                log.info("SOFT RESET the node.");
                node.getJedis().clusterReset(Reset.SOFT);
            } else if (StringUtils.equalsIgnoreCase(reset, "hard")) {
                log.info("HARD RESET the node.");
                node.getJedis().clusterReset(Reset.HARD);
            }
        }
        if (shutdown) {
            log.info("SHUTDOWN the node.");
            node.getJedis().shutdown();
        }

        // Give one 3 second for gossip
        Thread.sleep(3000);
    }

    public void deleteFailNodeOfCluster(final String hostAndPort, String nodeId)
            throws Exception {
        ValidationUtils.notBlank(nodeId, "nodeId");

        log.info("Removing node({}) from cluster({}).", nodeId, hostAndPort);

        // Load cluster information
        loadClusterInfoFromNode(hostAndPort);

        // Send CLUSTER FORGET to all the nodes but the node to remove
        log.info("Sending CLUSTER FORGET messages to the cluster...");
        nodes.stream().filter(v -> {
            return !StringUtils.equalsIgnoreCase(v.getNodeInfo().getNodeId(), nodeId);
        }).forEach(v -> {
            if (StringUtils.isNotBlank(v.getNodeInfo().getMasterNodeId())
                    && StringUtils.equalsIgnoreCase(v.getNodeInfo().getMasterNodeId(), nodeId)) {
                // Reconfigure the slave to replicate with some other node
                TribClusterNode master = getMasterWithLeastReplicasSpecifiedNodeIdExcluded(nodeId);
                log.info("new master={}, old master = {}", master.getNodeInfo().getNodeId(), nodeId);
                log.info("{} as replica of {}", v.getNodeInfo().getNodeId(), master.getNodeInfo().getNodeId());
                v.getJedis().clusterReplicate(master.getNodeInfo().getNodeId());
            }
            v.getJedis().clusterForget(nodeId);
        });

        // Give one 3 second for gossip
        Thread.sleep(3000);
    }

    public void replicateNode(final String hostAndPort, final String masterNodeId)
            throws Exception {
        ValidationUtils.notBlank(masterNodeId, "masterNodeId");

        log.info("Replicate node({}) as slave of {}.", hostAndPort, masterNodeId);

        loadClusterInfoFromNode(hostAndPort);
        checkCluster();

        TribClusterNode node = getNodeByHostAndPort(hostAndPort);
        if (node == null) {
            throw new InvalidParameterException(String.format("Node(%s) does not exists.", hostAndPort));
        }
        if (node.hasFlag("slave")) {
            // OK
        } else {
            if (node.getNodeInfo().getServedSlotsSet().size() > 0) {
                throw new InvalidParameterException(String.format("Node(%s) is not empty! Reshard data away and try again.", hostAndPort));
            }
        }

        TribClusterNode master = getNodeByNodeId(masterNodeId);
        if (master == null) {
            throw new InvalidParameterException(String.format("Node(%s) does not exists.", masterNodeId));
        }
        if (!master.hasFlag("master")) {
            throw new InvalidParameterException(String.format("Node(%s) is not master.", masterNodeId));
        }

        log.info("Configure node({}) as replica of {}.", hostAndPort, masterNodeId);
        node.getJedis().clusterReplicate(masterNodeId);

        // Give one 3 second for gossip
        Thread.sleep(3000);
    }

    public void failoverNode(final String hostAndPort)
            throws Exception {
        log.info("Failover node({}).", hostAndPort);

        loadClusterInfoFromNode(hostAndPort);
        checkCluster();

        TribClusterNode node = getNodeByHostAndPort(hostAndPort);
        if (node == null) {
            throw new InvalidParameterException(String.format("Node(%s) does not exists.", hostAndPort));
        }
        if (node.hasFlag("slave")) {
            // OK
        } else {
            throw new InvalidParameterException(String.format("Node(%s) is not slave.", hostAndPort));
        }

        node.getJedis().clusterFailover();

        // Give one 3 second for gossip
        Thread.sleep(3000);
    }

    TribClusterNode getMasterWithLeastReplicas() {
        List<TribClusterNode> masters = nodes.stream().filter(node -> node.hasFlag("master")).collect(Collectors.toList());
        masters.sort((o1, o2) -> Integer.compare(o1.getNodeInfo().getServedSlotsSet().size(), o2.getNodeInfo().getServedSlotsSet().size()));
        return masters.get(0);
    }

    TribClusterNode getMasterWithLeastReplicasSpecifiedNodeIdExcluded(String nodeId) {
        List<TribClusterNode> masters = nodes.stream().filter(node -> node.hasFlag("master")).filter(node -> !StringUtils.equalsIgnoreCase(node.getNodeInfo().getNodeId(), nodeId)).collect(Collectors.toList());
        masters.sort((o1, o2) -> Integer.compare(o1.getNodeInfo().getServedSlotsSet().size(), o2.getNodeInfo().getServedSlotsSet().size()));
        return masters.get(0);
    }

    // Given a list of source nodes return a "resharding plan"
    // with what slots to move in order to move "numslots" slots to another
    // instance.
    List<ReshardTable> computeReshardTable(List<TribClusterNode> sources, int slotCount) {
        List<ReshardTable> moved = Lists.newArrayList();

        // Sort from bigger to smaller instance, for two reasons:
        // 1) If we take less slots than instances it is better to start
        //    getting from the biggest instances.
        // 2) We take one slot more from the first instance in the case of not
        //    perfect divisibility. Like we have 3 nodes and need to get 10
        //    slots, we take 4 from the first, and 3 from the rest. So the
        //    biggest is always the first.
        sources.sort((o1, o2) -> {
            return Integer.compare(o2.getNodeInfo().getServedSlotsSet().size(), o1.getNodeInfo().getServedSlotsSet().size());
        });
        int sourceTotalSlot = sources.stream().mapToInt(source -> source.getNodeInfo().getServedSlotsSet().size()).sum();
        if (sourceTotalSlot < slotCount) {
            throw new InvalidParameterException(String.format("Total slot count which is sum of from nodes is not enough. Slot count is %s, But total slot count is %s.", slotCount, sourceTotalSlot));
        }

        int i = 0;
        for (TribClusterNode source : sources) {
            // Every node will provide a number of slots proportional to the
            // slots it has assigned.
            double n = (double) slotCount / (double) sourceTotalSlot
                    * (double) source.getNodeInfo().getServedSlotsSet().size();
            if (i == 0) {
                n = Math.ceil(n);
            } else {
                n = Math.floor(n);
            }

            int j = 0;
            for (Integer slot : source.getNodeInfo().getServedSlotsSet()) {
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

    void clusterError(String error) {
        log.warn(error);
        errors.add(error);
    }

    List<String> checkCluster() {
        // init error
        errors = Lists.newArrayList();

        checkConfigConsistency();
        checkOpenSlots();
        // TODO: checkSlotCoverage

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
        Set<String> signatures = Sets.newHashSet();
        nodes.forEach(node -> {
            String signature = node.getConfigSignature();
            signatures.add(signature);
        });
        return signatures.size() == 1;
    }

    void checkOpenSlots() {
        log.info("Check for open slots.");
        Set<Integer> openSlots = Sets.newTreeSet();
        for (TribClusterNode node : nodes) {
            if (node.getNodeInfo().getMigrating().size() > 0) {
                clusterError(String.format("[Warning] Node %s has slots in migrating state. (%s).", node.getNodeInfo().getHostAndPort(), StringUtils.join(node.getNodeInfo().getMigrating().keySet(), ",")));
                openSlots.addAll(node.getNodeInfo().getMigrating().keySet());
            } else if (node.getNodeInfo().getImporting().size() > 0) {
                clusterError(String.format("[Warning] Node %s has slots in importing state (%s).", node.getNodeInfo().getHostAndPort(), StringUtils.join(node.getNodeInfo().getImporting().keySet(), ",")));
                openSlots.addAll(node.getNodeInfo().getImporting().keySet());
            }
        }
        if (openSlots.size() > 0) {
            log.warn("The following slots are open: {}", StringUtils.join(openSlots, ","));
        }
        if (fix) {
            openSlots.forEach(v -> {
                fixOpenSlot(Integer.valueOf(v));
            });
        }
    }

    /**
     * Slot 'slot' was found to be in importing or migrating state in one or<br>
     * more nodes. This function fixes this condition by migrating keys where<br>
     * it seems more sensible.
     *
     * @param slot
     */
    void fixOpenSlot(int slot) {
        log.info("Fixing open slots. {}", slot);

        TribClusterNode owner = getSlotOwner(slot);
        if (owner == null) {
            throw new ApiException(Constants.ERR_CODE_UNKNOWN, "Fix me, some work to do here.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<TribClusterNode> migrating = Lists.newArrayList();
        List<TribClusterNode> importing = Lists.newArrayList();
        for (TribClusterNode node : nodes) {
            if (node.hasFlag("slave")) {
                continue;
            }
            if (node.getNodeInfo().getMigrating().containsKey(Integer.valueOf(slot))) {
                migrating.add(node);
            } else if (node.getNodeInfo().getImporting().containsKey(Integer.valueOf(slot))) {
                importing.add(node);
            } else if (node.getJedis().clusterCountKeysInSlot(slot) > 0
                    && !StringUtils.equalsIgnoreCase(node.getNodeInfo().getNodeId(), owner.getNodeInfo().getNodeId())) {
                log.info("Found keys about slot {} in node {}", slot, node.getNodeInfo().getHostAndPort());
                importing.add(node);
            }
        }

        log.info("Set as migrating in: {}", migrating.stream().map(v -> v.getNodeInfo().getHostAndPort()));
        log.info("Set as migrating in: {}", importing.stream().map(v -> v.getNodeInfo().getHostAndPort()));

        // Case 1: The slot is in migrating state in one slot, and in
        //         importing state in 1 slot. That's trivial to address.
        if (migrating.size() == 1 && importing.size() == 1) {
            moveSlot(migrating.get(0), importing.get(0), slot, true, false);
        } else if (migrating.size() == 0 && importing.size() > 0) {
            log.info("Moving all the {} slot keys to its owner {}", slot, owner.getNodeInfo().getHostAndPort());
            for (TribClusterNode importingNode : importing) {
                if (StringUtils.equalsIgnoreCase(importingNode.getNodeInfo().getNodeId(), owner.getNodeInfo().getNodeId())) {
                    continue;
                }
                moveSlot(importingNode, owner, slot, true, true);
                log.info("Setting {} as STABLE in {}", slot, importingNode.getNodeInfo().getNodeId());
                importingNode.getJedis().clusterSetSlotStable(slot);
            }
        } else {
            throw new ApiException(Constants.ERR_CODE_UNKNOWN, "Sorry, we can't fix this slot yet (work in progress)", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Move slots between source and target nodes using MIGRATE.<br>
     * Options:<br>
     * :fix     -- We are moving in the context of a fix. Use REPLACE.<br>
     * :cold    -- Move keys without opening / reconfiguring the nodes.<br>
     *
     * @param source
     * @param target
     * @param slot
     * @param fix
     * @param cold
     */
    void moveSlot(TribClusterNode source, TribClusterNode target, int slot, boolean fix, boolean cold) {
        // We start marking the slot as importing in the destination node,
        // and the slot as migrating in the target host. Note that the order of
        // the operations is important, as otherwise a client may be redirected
        // to the target node that does not yet know it is importing this slot.
        log.info("Moving slot {} from {} to {}.", slot, source.getNodeInfo().getHostAndPort(), target.getNodeInfo().getHostAndPort());

        if (!cold) {
            target.getJedis().clusterSetSlotImporting(slot, source.getNodeInfo().getNodeId());
            source.getJedis().clusterSetSlotMigrating(slot, target.getNodeInfo().getNodeId());
        }

        while (true) {
            List<String> keys = source.getJedis().clusterGetKeysInSlot(slot, 10);
            if (keys.size() == 0) {
                break;
            }
            keys.forEach(key -> {
                try {
                    source.getJedis().migrate(target.getNodeInfo().getHost(), target.getNodeInfo().getPort(), key, 0, 15000);
                } catch (Exception e) {
                    if (fix && StringUtils.contains(e.getMessage(), "BUSYKEY")) {
                        // TODO: i want to replcate. but jedis not have replace.
                        log.error("Error.", e);
                        throw new ApiException(Constants.ERR_CODE_UNKNOWN, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                    } else {
                        log.error("Error.", e);
                        throw new ApiException(Constants.ERR_CODE_UNKNOWN, e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }
                }
            });
        }

        if (!cold) {
            nodes.forEach(node -> {
                node.getJedis().clusterSetSlotNode(slot, target.getNodeInfo().getNodeId());
            });
        }
    }

    TribClusterNode getSlotOwner(int slot) {
        return nodes.stream().filter(node -> {
            return node.getNodeInfo().getServedSlotsSet().contains(slot);
        }).findFirst().orElse(null);
    }

    // TODO:
    //	void checkSlotsCoverage() {
    //		log.info("Check slots coverage.");
    //		Set<Integer> slots = coveredSlots();
    //		if (slots.size() == Constants.ALL_SLOTS_SIZE) {
    //			log.info("All 16384 slots covered.");
    //		} else {
    //			log.warn("Not all 16384 slots covered.");
    //			if (fix) {
    //				fixSlotsCoverage();
    //			}
    //		}
    //	}
    //
    //	Set<Integer> coveredSlots() {
    //		Set<Integer> slots = Sets.newTreeSet();
    //		nodes.forEach(node -> {
    //			slots.addAll(node.getNodeInfo().getServedSlotsSet());
    //		});
    //		return slots;
    //	}
    //
    //	void fixSlotsCoverage() {
    //		Set<Integer> notCovered = Sets.newTreeSet();
    //		for (int i = 0; i < Constants.ALL_SLOTS_SIZE; i++) {
    //			notCovered.add(i);
    //		}
    //		notCovered.removeAll(coveredSlots());
    //		log.info("Fixing slots coverage.");
    //		log.info("List of not covered slots: {}", StringUtils.join(notCovered, ","));
    //
    //		// For every slot, take action depending on the actual condition:
    //		// 1) No node has keys for this slot.
    //		// 2) A single node has keys for this slot.
    //		// 3) Multiple nodes have keys for this slot.
    //		Set<Integer> slots = Sets.newTreeSet();
    //
    //	}

    void flushNodesConfig() {
        nodes.forEach(node -> {
            node.flushNodeConfig();
        });
    }

    @SuppressWarnings("unused")
    void assignConfigEpoch() {
        int configEpoch = 1;
        for (TribClusterNode node : nodes) {
            try {
                //TODO: SET CONFIG EPOCH
            } catch (Exception e) {
            }
            configEpoch++;
        }
    }

    void joinCluster() {
        TribClusterNode firstNode = null;
        for (TribClusterNode node : nodes) {
            if (firstNode == null) {
                firstNode = node;
            } else {
                node.getJedis().clusterMeet(firstNode.getNodeInfo().getHost(), firstNode.getNodeInfo().getPort());
            }
        }
    }

    void waitClusterJoin() throws Exception {
        log.info("Waiting for the cluster join.");
        String waiting = ".";
        while (!isConfigConsistent()) {
            log.info(waiting);
            waiting += ".";
            Thread.sleep(1000);
        }
    }

    void checkCreateParameters() {
        int masters = nodes.size() / (replicas + 1);
        if (masters < 3) {
            throw new InvalidParameterException("Redis Cluster requires at least 3 master nodes.");
        }
    }

    void addNodes(TribClusterNode node) {
        nodes.add(node);
    }

    void allocSlots() {
        int mastersCount = nodes.size() / (replicas + 1);
        List<TribClusterNode> masters;

        // The first step is to split instances by IP. This is useful as
        // we'll try to allocate master nodes in different physical machines
        // (as much as possible) and to allocate slaves of a given master in
        // different physical machines as well.
        //
        // This code assumes just that if the IP is different, than it is more
        // likely that the instance is running in a different physical host
        // or at least a different virtual machine.
        Map<String, List<TribClusterNode>> ips = Maps.newTreeMap();
        ips.putAll(nodes.stream().collect(Collectors.groupingBy(v -> v.getNodeInfo().getHost(), Collectors.toList())));
        // sort hostNodes
        for (Map.Entry<String, List<TribClusterNode>> e : ips.entrySet()) {
            List<TribClusterNode> hostNodes = e.getValue();
            hostNodes.sort((o1, o2) -> {
                if (StringUtils.equals(o1.getNodeInfo().getHost(), o2.getNodeInfo().getHost())) {
                    return Integer.compare(o1.getNodeInfo().getPort(), o2.getNodeInfo().getPort());
                } else {
                    return o1.getNodeInfo().getHost().compareTo(o2.getNodeInfo().getHost());
                }
            });
        }

        log.info("Select master instances.");
        List<TribClusterNode> interleaved = Lists.newArrayList();
        boolean stop = false;
        while (!stop) {
            // Take one node from each IP until we run out of nodes
            // across every IP.
            for (Map.Entry<String, List<TribClusterNode>> e : ips.entrySet()) {
                List<TribClusterNode> hostNodes = e.getValue();

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
        double slotsPerNode = Constants.ALL_SLOTS_SIZE / mastersCount;
        int first = 0;
        int last;
        double cursor = 0.0;
        for (int i = 0; i < masters.size(); i++) {
            last = (int) Math.round(cursor + slotsPerNode - 1.0);
            if (last > Constants.ALL_SLOTS_SIZE || i == masters.size() - 1) {
                last = Constants.ALL_SLOTS_SIZE - 1;
            }
            if (last < first) { // Min step is 1
                last = first;
            }

            Set<Integer> slots = Sets.newTreeSet();
            IntStream.rangeClosed(first, last).forEach(v -> slots.add(v));
            log.info("Add slots to {}. first={}, last={}", masters.get(i), first, last);
            masters.get(i).addTmpSlots(slots);

            first = last + 1;
            cursor = cursor + slotsPerNode;
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
        Map<TribClusterNode, List<TribClusterNode>> replicasOfMaster = Maps.newLinkedHashMap();
        for (TribClusterNode master : masters) {
            replicasOfMaster.put(master, Lists.newArrayList());
            while (replicasOfMaster.get(master).size() < replicas) {
                if (interleaved.size() == 0) {
                    log.info("break because node count is 0");
                    break;
                }

                // Return the first node not matching our current master
                TribClusterNode replicaNode;
                Optional<TribClusterNode> replicaNodeOp;

                replicaNodeOp = interleaved.stream()
                        .filter(v -> {
                            boolean isNotSameHostAsMaster = !StringUtils.equals(master.getNodeInfo().getHost(), v.getNodeInfo().getHost());
                            boolean isSameHostAsAlreadyRegisteredReplica = replicasOfMaster.get(master).stream()
                                    .anyMatch(rpl -> StringUtils.equals(v.getNodeInfo().getHost(), rpl.getNodeInfo().getHost()));
                            return isNotSameHostAsMaster && !isSameHostAsAlreadyRegisteredReplica;
                        }).findFirst();

                if (replicaNodeOp.isPresent()) {
                    replicaNode = replicaNodeOp.get();
                    interleaved.remove(replicaNode);
                } else {
                    replicaNode = interleaved.remove(0);
                }
                log.info("Select {} as replica of {}", replicaNode, master);
                replicaNode.setAsReplica(master.getNodeInfo().getNodeId());
                replicasOfMaster.get(master).add(replicaNode);
            }
        }

        // we want to attach different host of master and min size.
        log.info("Select extra replica.");
        for (TribClusterNode extra : interleaved) {
            TribClusterNode master;
            Optional<Entry<TribClusterNode, List<TribClusterNode>>> entrySetNodeOp;

            int sameHostOfMasterCount = (int)masters.stream()
                    .filter(v -> StringUtils.equals(v.getNodeInfo().getHost(), extra.getNodeInfo().getHost()))
                    .count();

            entrySetNodeOp = replicasOfMaster.entrySet().stream()
                    .min((e1, e2) -> Integer.compare(e1.getValue().size(), e2.getValue().size()));
            int minSize = entrySetNodeOp.get().getValue().size();
            int sameMinCount = (int)replicasOfMaster.entrySet().stream()
                    .filter(v -> minSize == v.getValue().size())
                    .count();

            if (sameMinCount > sameHostOfMasterCount) {
                entrySetNodeOp = replicasOfMaster.entrySet().stream()
                        .filter(e -> !StringUtils.equals(e.getKey().getNodeInfo().getHost(), extra.getNodeInfo().getHost()))
                        .min((e1, e2) -> Integer.compare(e1.getValue().size(), e2.getValue().size()));
            }

            master = entrySetNodeOp.get().getKey();
            log.info("Select {} as replica of {} (extra)", extra, master);
            extra.setAsReplica(master.getNodeInfo().getNodeId());
            replicasOfMaster.get(master).add(extra);
        }
    }

    List<CreateClusterParam> buildCreateClusterParam() {
        // first loop master
        List<CreateClusterParam> params = Lists.newArrayList();
        nodes.stream()
                .filter(node -> StringUtils.isBlank(node.getNodeInfo().getMasterNodeId()))
                .forEach(node -> {
                    int startSlotNumber = node.getTmpSlots().stream().min(Integer::compare).get();
                    int endSlotNumber = node.getTmpSlots().stream().max(Integer::compare).get();

                    CreateClusterParam param = new CreateClusterParam();
                    param.setStartSlotNumber(String.valueOf(startSlotNumber));
                    param.setEndSlotNumber(String.valueOf(endSlotNumber));
                    param.setMaster(node.getNodeInfo().getHostAndPort());
                    param.setMasterNodeId(node.getNodeInfo().getNodeId());
                    params.add(param);
                });

        // replica loop
        nodes.stream()
                .filter(node -> StringUtils.isNotBlank(node.getNodeInfo().getMasterNodeId()))
                .forEach(node -> {
                    String masterNodeId = node.getNodeInfo().getMasterNodeId();
                    params.stream().filter(param -> StringUtils.equals(param.getMasterNodeId(), masterNodeId)).forEach(param -> {
                        List<String> replicaList = param.getReplicas();
                        if (param.getReplicas() == null) {
                            replicaList = Lists.newArrayList();
                            param.setReplicas(replicaList);
                        }
                        replicaList.add(node.getNodeInfo().getHostAndPort());
                    });
                });

        // sort
        params.sort((v1, v2) -> Integer.compare(Integer.valueOf(v1.getStartSlotNumber()), Integer.valueOf(v2.getStartSlotNumber())));

        return params;
    }

    void loadClusterInfoFromNode(String hostAndPort) {
        ValidationUtils.hostAndPort(hostAndPort);

        TribClusterNode node = new TribClusterNode(hostAndPort);
        node.connect(true);
        node.assertCluster();
        node.loadInfo(true);
        addNodes(node);

        for (ClusterNode friend : node.getFriends()) {
            if (friend.hasFlag("noaddr") || friend.hasFlag("disconnected") || friend.hasFlag("fail")) {
                continue;
            }
            TribClusterNode friendTribNode = new TribClusterNode(friend.getHostAndPort());
            friendTribNode.connect();
            if (friendTribNode.getJedis() == null) {
                continue;
            }
            try {
                friendTribNode.loadInfo();
                addNodes(friendTribNode);
            } catch (Exception e) {
                log.error("Unable to load info for node {}.", friend.getHostAndPort());
            }
        }

        populateNodesReplicasInfo();
    }

    void populateNodesReplicasInfo() {
        // Populate the replicas field using the replicate field of slave
        // nodes.
        nodes.forEach(node -> {
            if (StringUtils.isNotBlank(node.getNodeInfo().getMasterNodeId())) {
                TribClusterNode master = getNodeByNodeId(node.getNodeInfo().getMasterNodeId());
                if (master == null) {
                    throw new ApiException(
                            Constants.ERR_CODE_UNKNOWN,
                            String.format(
                                    "%s claims to be slave of unknown node ID %s.",
                                    node.getNodeInfo().getHostAndPort(),
                                    node.getNodeInfo().getMasterNodeId()
                            ),
                            HttpStatus.INTERNAL_SERVER_ERROR
                    );
                }
                master.getReplicas().add(node);
            }
        });
    }

    TribClusterNode getNodeByNodeId(String nodeId) {
        return nodes.stream()
                .filter(node -> StringUtils.equalsIgnoreCase(node.getNodeInfo().getNodeId(), nodeId))
                .findFirst()
                .orElse(null);
    }

    TribClusterNode getNodeByHostAndPort(String hostAndPort) {
        return nodes.stream()
                .filter(node -> StringUtils.equalsIgnoreCase(node.getNodeInfo().getHostAndPort(), hostAndPort))
                .findFirst()
                .orElse(null);
    }

    TribClusterNode createTribClusterNode(String hostAndPort) {
        return new TribClusterNode(hostAndPort);
    }

    @Override
    public void close() throws IOException {
        if (nodes != null) {
            nodes.forEach(v -> {
                try {
                    v.close();
                } catch (Exception e) {
                    log.error("Failed to close redis connection.");
                }
            });
        }
    }

    @Getter
    @Setter
    public static class ReshardTable {
        private TribClusterNode source;
        private int slot;

        public ReshardTable(TribClusterNode source, int slot) {
            this.source = source;
            this.slot = slot;
        }
    }
}
