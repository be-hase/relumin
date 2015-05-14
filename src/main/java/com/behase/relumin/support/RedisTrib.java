package com.behase.relumin.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

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

/**
 * Almost Clone of redis-trib.rb
 * @author Ryosuke Hasebe
 *
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
				replicaNode.setAsReplica(masterNode.getInfo().getNodeId());
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
	}

	public void checkCluster(String hostAndPort) {
		loadClusterInfoFromNode(hostAndPort);
		checkCluster();
	}

	public void fixCluster(String hostAndPort) {
		fix = true;
		loadClusterInfoFromNode(hostAndPort);
		checkCluster();
	}

	public void reshardCluster(String hostAndPort, int slotCount, String fromNodeIds, String toNodeId) {
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
				if (StringUtils.equals(node.getInfo().getNodeId(), target.getInfo().getNodeId())) {
					continue;
				}
				if (node.hasFlag("slave")) {
					continue;
				}
				sources.add(node);
			}
		} else {
			for (String fromNodeId : StringUtils.split(fromNodeIds, ",")) {
				TribClusterNode fromNode = getNodeByNodeId(fromNodeId);
				if (fromNode == null || fromNode.hasFlag("slave")) {
					throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "The specified node is not known or is not a master, please retry.", HttpStatus.BAD_REQUEST);
				}
				sources.add(fromNode);
			}
		}

		if (sources.stream().filter(source -> {
			return StringUtils.equals(source.getInfo().getNodeId(), target.getInfo().getNodeId());
		}).findAny().isPresent()) {
			throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Target node is also listed among the source nodes!", HttpStatus.BAD_REQUEST);
		}

		log.info("Ready to move {} slots.", slotCount);
		log.info("Source nodes : {}", sources.stream().map(v -> String.format("%s (%s)", v.getInfo().getHostAndPort(), v.getInfo().getNodeId())).collect(Collectors.toList()));
		log.info("Destination nodes : {}", String.format("%s (%s)", target.getInfo().getHostAndPort(), target.getInfo().getNodeId()));
		List<ReshardTable> reshardTables = computeReshardTable(sources, slotCount);
		reshardTables.forEach(reshardTable -> {
			log.debug("{}, {}", reshardTable.getSource().getInfo().getHostAndPort(), reshardTable.getSlot());
			moveSlot(reshardTable.getSource(), target, reshardTable.getSlot(), false, false);
		});
	}

	public void addNodeIntoCluster(String hostAndPort, String newHostAndPort) {
		ValidationUtils.hostAndPort(hostAndPort);
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
		TribClusterNode first = nodes.get(0);
		log.info("Send CLUSTER MEET to node {} to make it join the cluster.", newHostAndPort);
		newNode.getJedis().clusterMeet(first.getInfo().getHost(), first.getInfo().getPort());

		log.info("New node added correctly.");
	}

	void addNodeIntoClusterAsReplica(String hostAndPort, String newHostAndPort, String materNodeId) throws Exception {
		ValidationUtils.hostAndPort(hostAndPort);
		ValidationUtils.hostAndPort(newHostAndPort);

		log.info("Adding node {} to cluster {}", newHostAndPort, hostAndPort);
		loadClusterInfoFromNode(hostAndPort);
		checkCluster();

		// If --master-id was specified, try to resolve it now so that we
		// abort before starting with the node configuration.
		TribClusterNode master;
		if (StringUtils.isBlank(materNodeId)) {
			master = getMasterWithLeastReplicas();
			log.info("Automatically selected master {} ().", master.getInfo().getHostAndPort(), master.getInfo().getMasterNodeId());
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
		TribClusterNode first = nodes.get(0);
		log.info("Send CLUSTER MEET to node {} to make it join the cluster.", newHostAndPort);
		newNode.getJedis().clusterMeet(first.getInfo().getHost(), first.getInfo().getPort());

		// Additional configuration is needed if the node is added as
		// a slave.
		waitClusterJoin();
		log.info("Configure node as replica of {}.", master.getInfo().getNodeId());
		newNode.getJedis().clusterReplicate(master.getInfo().getNodeId());

		log.info("New node added correctly.");
	}

	TribClusterNode getMasterWithLeastReplicas() {
		List<TribClusterNode> masters = nodes.stream().filter(node -> node.hasFlag("master")).collect(Collectors.toList());
		masters.sort((o1, o2) -> Integer.compare(o1.getInfo().getServedSlotsSet().size(), o2.getInfo().getServedSlotsSet().size()));
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
			return Integer.compare(o2.getInfo().getServedSlotsSet().size(), o1.getInfo().getServedSlotsSet().size());
		});
		int sourceTotalSlot = sources.stream().mapToInt(source -> source.getInfo().getServedSlotsSet().size()).sum();
		log.debug("sourceTotalSlot : {}", sourceTotalSlot);

		int i = 0;
		for (TribClusterNode source : sources) {
			// Every node will provide a number of slots proportional to the
			// slots it has assigned.
			double n = (double)slotCount / (double)sourceTotalSlot
				* (double)source.getInfo().getServedSlotsSet().size();
			if (i == 0) {
				n = Math.ceil(n);
			} else {
				n = Math.floor(n);
			}

			int j = 0;
			for (Integer slot : source.getInfo().getServedSlotsSet()) {
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

	void checkCluster() {
		checkConfigConsistency();
		checkOpenSlots();
		// TODO: checkSlotCoverage
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
			log.debug("signature={}", signature);
			signatures.add(signature);
		});
		return signatures.size() == 1;
	}

	void checkOpenSlots() {
		log.info("Check for open slots.");
		Set<Integer> openSlots = Sets.newTreeSet();
		for (TribClusterNode node : nodes) {
			if (node.getInfo().getMigrating().size() > 0) {
				clusterError(String.format("[Warning] Node %s has slots in migrating state. (%s).", node.getInfo().getHostAndPort(), StringUtils.join(node.getInfo().getMigrating().keySet(), ",")));
				openSlots.addAll(node.getInfo().getMigrating().keySet());
			} else if (node.getInfo().getImporting().size() > 0) {
				clusterError(String.format("[Warning] Node %s has slots in importing state (%s).", node.getInfo().getHostAndPort(), StringUtils.join(node.getInfo().getImporting().keySet(), ",")));
				openSlots.addAll(node.getInfo().getImporting().keySet());
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
	 * @param slot
	 */
	void fixOpenSlot(int slot) {
		log.info("Fixing open slots. {}", slot);

		TribClusterNode owner = getSlotOwner(slot);
		if (owner == null) {
			throw new ApiException(Constants.ERR_CODE_UNKNOWN, "*** Fix me, some work to do here.", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		List<TribClusterNode> migrating = Lists.newArrayList();
		List<TribClusterNode> importing = Lists.newArrayList();
		for (TribClusterNode node : nodes) {
			if (node.hasFlag("slave")) {
				continue;
			}
			if (node.getInfo().getMigrating().containsKey(Integer.valueOf(slot))) {
				migrating.add(node);
			} else if (node.getInfo().getImporting().containsKey(Integer.valueOf(slot))) {
				importing.add(node);
			} else if (node.getJedis().clusterCountKeysInSlot(slot) > 0
				&& !StringUtils.equals(node.getInfo().getNodeId(), owner.getInfo().getNodeId())) {
				log.info("*** Found keys about slot {} in node {}", slot, node.getInfo().getHostAndPort());
				importing.add(node);
			}
		}

		log.info("Set as migrating in: {}", migrating.stream().map(v -> v.getInfo().getHostAndPort()));
		log.info("Set as migrating in: {}", importing.stream().map(v -> v.getInfo().getHostAndPort()));

		// Case 1: The slot is in migrating state in one slot, and in
		//         importing state in 1 slot. That's trivial to address.
		if (migrating.size() == 1 && importing.size() == 1) {
			moveSlot(migrating.get(0), importing.get(0), slot, true, false);
		} else if (migrating.size() == 0 && importing.size() > 0) {
			log.info("Moving all the {} slot keys to its owner {}", slot, owner.getInfo().getHostAndPort());
			for (TribClusterNode importingNode : importing) {
				if (StringUtils.equals(importingNode.getInfo().getNodeId(), owner.getInfo().getNodeId())) {
					continue;
				}
				moveSlot(importingNode, owner, slot, true, true);
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
		log.info("Moving slot {} from {} to {}.", slot, source.getInfo().getHostAndPort(), target.getInfo().getHostAndPort());

		if (!cold) {
			target.getJedis().clusterSetSlotImporting(slot, source.getInfo().getNodeId());
			source.getJedis().clusterSetSlotMigrating(slot, target.getInfo().getNodeId());
		}

		while (true) {
			List<String> keys = source.getJedis().clusterGetKeysInSlot(slot, 10);
			if (keys.size() == 0) {
				break;
			}
			keys.forEach(key -> {
				try {
					source.getJedis().migrate(target.getInfo().getHost(), target.getInfo().getPort(), key, 0, 15000);
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
				node.getJedis().clusterSetSlotNode(slot, target.getInfo().getNodeId());
			});
		}
	}

	TribClusterNode getSlotOwner(int slot) {
		return nodes.stream().filter(node -> {
			return node.getInfo().getServedSlotsSet().contains(slot);
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
	//			slots.addAll(node.getInfo().getServedSlotsSet());
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
				node.getJedis().clusterMeet(firstNode.getInfo().getHost(), firstNode.getInfo().getPort());
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
		log.debug("masters = {}", masters);
		if (masters < 3) {
			throw new InvalidParameterException("Redis Cluster requires at least 3 master nodes.");
		}
	}

	void addNodes(TribClusterNode node) {
		nodes.add(node);
	}

	void allocSlots() {
		int mastersCount = nodes.size() / (replicas + 1);
		List<TribClusterNode> masters = Lists.newArrayList();

		// The first step is to split instances by IP. This is useful as
		// we'll try to allocate master nodes in different physical machines
		// (as much as possible) and to allocate slaves of a given master in
		// different physical machines as well.
		// 
		// This code assumes just that if the IP is different, than it is more
		// likely that the instance is running in a different physical host
		// or at least a different virtual machine.
		Map<String, List<TribClusterNode>> tmpIps = nodes.stream().collect(Collectors.groupingBy(v -> v.getInfo().getHost(), Collectors.toList()));
		Map<String, List<TribClusterNode>> ips = Maps.newTreeMap();
		ips.putAll(tmpIps);
		for (Map.Entry<String, List<TribClusterNode>> e : ips.entrySet()) {
			List<TribClusterNode> hostNodes = e.getValue();
			hostNodes.sort((o1, o2) -> {
				if (StringUtils.equals(o1.getInfo().getHost(), o2.getInfo().getHost())) {
					return Integer.compare(o1.getInfo().getPort(), o2.getInfo().getPort());
				} else {
					return o1.getInfo().getHost().compareTo(o2.getInfo().getHost());
				}
			});
		}
		log.debug("ip and nodes={}", ips);

		// Select master instances
		List<TribClusterNode> interleaved = Lists.newArrayList();
		boolean stop = false;
		while (!stop) {
			for (Map.Entry<String, List<TribClusterNode>> e : ips.entrySet()) {
				List<TribClusterNode> hostNodes = e.getValue();

				if (hostNodes.isEmpty()) {
					if (interleaved.size() == nodes.size()) {
						stop = true;
						continue;
					}
				} else {
					interleaved.add(hostNodes.remove(0));
				}
			}
		}

		masters = Lists.newArrayList(interleaved.subList(0, mastersCount));
		interleaved = Lists.newArrayList(interleaved.subList(mastersCount, interleaved.size()));
		log.info("masters={}", masters);
		log.info("interleaved={}", interleaved);

		// Alloc slots on masters
		double slotsPerNode = Constants.ALL_SLOTS_SIZE / mastersCount;
		int first = 0;
		int last;
		double cursor = 0.0;
		for (int i = 0; i < masters.size(); i++) {
			last = (int)Math.round(cursor + slotsPerNode - 1.0);
			if (last > Constants.ALL_SLOTS_SIZE || i == masters.size() - 1) {
				last = Constants.ALL_SLOTS_SIZE - 1;
			}
			if (last < first) { // Min step is 1
				last = first;
			}

			Set<Integer> slots = Sets.newTreeSet();
			for (int j = first; j <= last; j++) {
				slots.add(j);
			}
			log.info("add slots to {}. first={}, last={}", masters.get(i), first, last);
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
			while (replicasOfMaster.get(master).size() < this.replicas) {
				if (interleaved.size() == 0) {
					log.info("break because node count is 0");
					break;
				}

				// Return the first node not matching our current master
				TribClusterNode replicaNode;
				Optional<TribClusterNode> replicaNodeOp;

				replicaNodeOp = interleaved.stream().filter(v -> {
					boolean notSameHostAsMaster = !StringUtils.equals(master.getInfo().getHost(), v.getInfo().getHost());
					boolean sameHostAsAlreadyRegisterdReplica = replicasOfMaster.get(master).stream().anyMatch(rpl -> {
						return StringUtils.equals(v.getInfo().getHost(), rpl.getInfo().getHost());
					});
					return notSameHostAsMaster && !sameHostAsAlreadyRegisterdReplica;
				}).findFirst();
				if (replicaNodeOp.isPresent()) {
					replicaNode = replicaNodeOp.get();
					interleaved.remove(replicaNode);
				} else {
					//					replicaNodeOp = interleaved.stream().filter(v -> {
					//						boolean notSameHostAsMaster = !StringUtils.equals(master.getInfo().getHost(), v.getInfo().getHost());
					//						return notSameHostAsMaster;
					//					}).findFirst();
					//					if (replicaNodeOp.isPresent()) {
					//						replicaNode = replicaNodeOp.get();
					//						interleaved.remove(replicaNode);
					//					} else {
					replicaNode = interleaved.remove(0);
					//					}
				}
				log.info("Select {} as replica of {}", replicaNode, master);
				replicaNode.setAsReplica(master.getInfo().getNodeId());
				replicasOfMaster.get(master).add(replicaNode);
			}
		}

		// want to attach different host of master and min size.
		log.info("Select extra replica.");
		for (TribClusterNode extra : interleaved) {
			TribClusterNode master;
			Optional<Entry<TribClusterNode, List<TribClusterNode>>> entrySetNodeOp;

			int sameHostOfMasterCount = (int)masters.stream().filter(v -> {
				return StringUtils.equals(v.getInfo().getHost(), extra.getInfo().getHost());
			}).count();

			entrySetNodeOp = replicasOfMaster.entrySet().stream().min((e1, e2) -> {
				return Integer.compare(e1.getValue().size(), e2.getValue().size());
			});
			int minSize = entrySetNodeOp.get().getValue().size();
			int sameMinCount = (int)replicasOfMaster.entrySet().stream().filter(v -> {
				return minSize == v.getValue().size();
			}).count();

			if (sameMinCount > sameHostOfMasterCount) {
				entrySetNodeOp = replicasOfMaster.entrySet().stream().filter(e -> {
					return !StringUtils.equals(e.getKey().getInfo().getHost(), extra.getInfo().getHost());
				}).min((e1, e2) -> {
					return Integer.compare(e1.getValue().size(), e2.getValue().size());
				});
			}

			master = entrySetNodeOp.get().getKey();
			log.info("Select {} as replica of {} (extra)", extra, master);
			extra.setAsReplica(master.getInfo().getNodeId());
			replicasOfMaster.get(master).add(extra);
		}
	}

	List<CreateClusterParam> buildCreateClusterParam() {
		// first loop master
		List<CreateClusterParam> params = Lists.newArrayList();
		nodes.stream().filter(node -> {
			return StringUtils.isBlank(node.getInfo().getMasterNodeId());
		}).forEach(node -> {
			int startSlotNumber = node.getTmpServedSlots().stream().min(Integer::compare).get();
			int endSlotNumber = node.getTmpServedSlots().stream().max(Integer::compare).get();

			CreateClusterParam param = new CreateClusterParam();
			param.setStartSlotNumber(String.valueOf(startSlotNumber));
			param.setEndSlotNumber(String.valueOf(endSlotNumber));
			param.setMaster(node.getInfo().getHostAndPort());
			param.setMasterNodeId(node.getInfo().getNodeId());
			params.add(param);
		});

		// replica loop
		nodes.stream().filter(node -> {
			return StringUtils.isNotBlank(node.getInfo().getMasterNodeId());
		}).forEach(node -> {
			String masterNodeId = node.getInfo().getMasterNodeId();
			params.stream().filter(param -> {
				return StringUtils.equals(param.getMasterNodeId(), masterNodeId);
			}).forEach(param -> {
				List<String> replicaList = param.getReplicas();
				if (param.getReplicas() == null) {
					replicaList = Lists.newArrayList();
					param.setReplicas(replicaList);
				}
				replicaList.add(node.getInfo().getHostAndPort());
			});
		});

		// sort
		params.sort((v1, v2) -> {
			return Integer.compare(Integer.valueOf(v1.getStartSlotNumber()), Integer.valueOf(v2.getStartSlotNumber()));
		});

		return params;
	}

	void loadClusterInfoFromNode(String hostAndPort) {
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
			if (StringUtils.isNotBlank(node.getInfo().getMasterNodeId())) {
				TribClusterNode master = getNodeByNodeId(node.getInfo().getMasterNodeId());
				if (master == null) {
					throw new ApiException(Constants.ERR_CODE_UNKNOWN, String.format("%s claims to be slave of unknown node ID %s.", node.getInfo().getHostAndPort(), node.getInfo().getMasterNodeId()), HttpStatus.INTERNAL_SERVER_ERROR);
				}
				master.getReplicas().add(node);
			}
		});
	}

	TribClusterNode getNodeByNodeId(String nodeId) {
		return nodes.stream().filter(node -> {
			return StringUtils.equalsIgnoreCase(node.getInfo().getNodeId(), nodeId);
		}).findFirst().orElse(null);
	}

	TribClusterNode getNodeByHostAndPort(String hostAndPort) {
		return nodes.stream().filter(node -> {
			return StringUtils.equalsIgnoreCase(node.getInfo().getHostAndPort(), hostAndPort);
		}).findFirst().orElse(null);
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
