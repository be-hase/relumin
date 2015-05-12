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
import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.util.ValidationUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

/**
 * Almost Clone of redis-trib.rb
 * @author Ryosuke Hasebe
 *
 */
@Slf4j
public class RedisTrib implements Closeable {
	private static final int CLUSTER_HASH_SLOTS = 16384;

	private int replicas;
	private List<TribClusterNode> nodes = Lists.newArrayList();

	public List<CreateClusterParam> getCreateClusterParams(int replicas, Set<String> hostAndPorts) {
		this.replicas = replicas;

		for (String hostAndPort : hostAndPorts) {
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

	public void checkCluster() {
		checkConfigConsistency();
	}

	void checkConfigConsistency() {
		if (isConfigConsistent()) {
			log.info("OK. All nodes agree about slots configuration.");
		} else {
			throw new ApiException(Constants.ERR_CODE_CLUSTER_NOT_AGREE_CONFIG, "Nodes don't agree about configuration!", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	void checkOpenSlots() {
		log.info("Check for open slots.");
	}

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

	boolean isConfigConsistent() {
		Set<String> signatures = Sets.newHashSet();
		nodes.forEach(node -> {
			String signature = node.getConfigSignature();
			log.debug("signature={}", signature);
			signatures.add(signature);
		});
		return signatures.size() == 1;
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
		double slotsPerNode = CLUSTER_HASH_SLOTS / mastersCount;
		int first = 0;
		int last;
		double cursor = 0.0;
		for (int i = 0; i < masters.size(); i++) {
			last = (int)Math.round(cursor + slotsPerNode - 1.0);
			if (last > CLUSTER_HASH_SLOTS || i == masters.size() - 1) {
				last = CLUSTER_HASH_SLOTS - 1;
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
}
