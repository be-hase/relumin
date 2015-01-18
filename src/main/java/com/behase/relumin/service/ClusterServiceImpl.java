package com.behase.relumin.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

import com.behase.relumin.Constants;
import com.behase.relumin.exception.ApiException;
import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.ClusterInfo;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.model.SlotInfo;
import com.behase.relumin.util.JedisUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClusterServiceImpl implements ClusterService {
	private static final String NAME_REGEX = "^[a-zA-Z0-9_-]{1,20}$";

	@Autowired
	JedisPool dataStoreJedisPool;

	@Autowired
	ObjectMapper mapper;

	@Override
	public Set<String> getClusters() {
		try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			return dataStoreJedis.smembers(Constants.REDIS_PREFIX + ".clusters");
		}
	}

	@Override
	public Cluster getCluster(String clusterName) throws IOException, ApiException {
		Cluster cluster = new Cluster();

		ClusterNode node = getActiveClusterNode(clusterName);

		try (Jedis jedis = JedisUtils.getJedisByHostAndPort(node.getHostAndPort())) {
			String clusterInfoResult = jedis.clusterInfo();
			ClusterInfo info = parseClusterInfoResult(clusterInfoResult);
			List<ClusterNode> nodes = parseClusterNodesResult(jedis.clusterNodes(), node.getHostAndPort());
			nodes.sort((o1, o2) -> o1.getHostAndPort().compareTo(o2.getHostAndPort()));

			List<SlotInfo> slots = Lists.newArrayList();
			Map<String, List<Map<String, String>>> slaveRelations = Maps.newHashMap();
			nodes.forEach(v -> {
				Map<String, String> nodeMap = Maps.newLinkedHashMap();
				nodeMap.put("nodeId", v.getNodeId());
				nodeMap.put("hostAndPort", v.getHostAndPort());

				boolean isMaster = StringUtils.isNotBlank(v.getServedSlots());
				if (isMaster) {
					String servedSlots = v.getServedSlots();
					String[] slotsRangesArray = StringUtils.split(servedSlots, ",");
					log.info("hoge : {}", servedSlots);
					for (String slotsRangeStr : slotsRangesArray) {
						String[] slotsRange = StringUtils.split(slotsRangeStr, "-");

						SlotInfo slotInfo = new SlotInfo();
						slotInfo.setStartSlotNumber(Integer.valueOf(slotsRange[0]));
						slotInfo.setEndSlotNumber(Integer.valueOf(slotsRange[1]));
						slotInfo.setMaster(nodeMap);
						slots.add(slotInfo);
					}
				} else {
					List<Map<String, String>> slaves = slaveRelations.get(v.getMasterNodeId());
					if (slaves == null) {
						slaves = Lists.newArrayList();
						slaveRelations.put(v.getMasterNodeId(), slaves);
					}
					slaves.add(nodeMap);
				}
			});
			slots.forEach(v -> {
				List<Map<String, String>> slaves = slaveRelations.get(v.getMaster().get("nodeId"));
				v.setSlaves(slaves);
			});
			slots.sort((o1, o2) -> Integer.compare(o1.getStartSlotNumber(), o2.getStartSlotNumber()));

			cluster.setInfo(info);
			cluster.setNodes(nodes);
			cluster.setSlots(slots);
			return cluster;
		}
	}

	@Override
	public void setCluster(String clusterName, String hostAndPort) throws ApiException, JsonProcessingException {
		validateClusterName(clusterName);
		validateNode(hostAndPort);

		try (
				Jedis jedis = JedisUtils.getJedisByHostAndPort(hostAndPort);
				Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			List<ClusterNode> nodes = parseClusterNodesResult(jedis.clusterNodes(), hostAndPort);

			dataStoreJedis.watch(Constants.REDIS_PREFIX + ".clusters", Constants.REDIS_PREFIX + ".cluster."
				+ clusterName);
			Transaction t = dataStoreJedis.multi();
			t.sadd(Constants.REDIS_PREFIX + ".clusters", clusterName);
			t.set(Constants.REDIS_PREFIX + ".cluster." + clusterName, mapper.writeValueAsString(nodes));
			t.exec();
		} catch (JedisException e) {
			log.warn("redis error.", e);
			throw new ApiException("400_002", "Redis Error. " + e.getMessage(), HttpStatus.BAD_REQUEST);
		}
	}

	@Override
	public void deleteCluster(String clusterName) {
		try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			Set<String> keys = dataStoreJedis.keys(Constants.REDIS_PREFIX + ".cluster." + clusterName + ".*");

			dataStoreJedis.watch(Constants.REDIS_PREFIX + ".clusters", Constants.REDIS_PREFIX + ".cluster."
				+ clusterName);
			Transaction t = dataStoreJedis.multi();
			t.srem(Constants.REDIS_PREFIX + ".clusters", clusterName);
			t.del(Constants.REDIS_PREFIX + ".cluster." + clusterName);
			if (keys.size() > 0) {
				t.del(keys.toArray(new String[keys.size()]));
			}
			t.exec();
		}
	}

	@Override
	public void refreshClusters() {
		Set<String> clusters = getClusters();
		for (String clusterName : clusters) {
			try {
				ClusterNode clusterNode = getActiveClusterNode(clusterName);
				setCluster(clusterName, clusterNode.getHostAndPort());
			} catch (Exception e) {
				log.error("refresh fail. {}", clusterName, e);
			}
		}
	}

	private ClusterNode getActiveClusterNode(String clusterName) throws IOException, ApiException {
		try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			String result = dataStoreJedis.get(Constants.REDIS_PREFIX + ".cluster." + clusterName);
			if (StringUtils.isBlank(result)) {
				throw new ApiException("404_001", "Not exists this cluster name.", HttpStatus.BAD_REQUEST);
			}

			List<ClusterNode> existCluterNodes = mapper.readValue(result, new TypeReference<List<ClusterNode>>() {
			});
			Collections.shuffle(existCluterNodes);

			for (ClusterNode clusterNode : existCluterNodes) {
				String[] hostAndPortArray = StringUtils.split(clusterNode.getHostAndPort(), ":");
				try (Jedis jedis = new Jedis(hostAndPortArray[0], Integer.valueOf(hostAndPortArray[1]), 200)) {
					if ("PONG".equalsIgnoreCase(jedis.ping())) {
						return clusterNode;
					}
				} catch (JedisException e) {
					log.warn("There is unconnect redis. The hostAndPort is {}", clusterNode.getHostAndPort());
				}
			}

			throw new ApiException("500_001", "All node is down.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private void validateClusterName(String name) throws ApiException {
		if (!name.matches(NAME_REGEX)) {
			throw new ApiException("400_001", "Name is invalid.", HttpStatus.BAD_REQUEST);
		}
	}

	private void validateNode(String node) throws ApiException {
		String[] hostAndPortArray = StringUtils.split(node, ":");
		if (hostAndPortArray.length != 2) {
			throw new ApiException("400_001", "Node is invalid.", HttpStatus.BAD_REQUEST);
		}
		try {
			Integer.valueOf(hostAndPortArray[1]);
		} catch (Exception e) {
			throw new ApiException("400_001", "Node's port is invalid.", HttpStatus.BAD_REQUEST);
		}
	}

	private List<ClusterNode> parseClusterNodesResult(String result, String hostAndPort) {
		List<ClusterNode> clusterNodes = Lists.newArrayList();
		for (String resultLine : StringUtils.split(result, "\n")) {
			ClusterNode clusterNode = new ClusterNode();

			String[] resultLineArray = StringUtils.split(resultLine);
			clusterNode.setNodeId(resultLineArray[0]);

			String eachHostAndPort = resultLineArray[1];
			String[] eachHostAndPortArray = StringUtils.split(eachHostAndPort, ":");
			if ("127.0.0.1".equals(eachHostAndPortArray[0]) || "localhost".equals(eachHostAndPortArray[0])) {
				clusterNode.setHostAndPort(hostAndPort);
			} else {
				clusterNode.setHostAndPort(eachHostAndPort);
			}

			String eachFlag = resultLineArray[2];
			List<String> eachFlagList = Arrays.asList(StringUtils.split(eachFlag, ","));
			clusterNode.setMaster(eachFlagList.contains("master"));
			clusterNode.setSlave(eachFlagList.contains("slave"));
			clusterNode.setFail(eachFlagList.contains("fail"));

			clusterNode.setMasterNodeId("-".equals(resultLineArray[3]) ? "" : resultLineArray[3]);

			clusterNode.setTimeLastPing(Long.valueOf(resultLineArray[4]));
			clusterNode.setTimeLastPong(Long.valueOf(resultLineArray[5]));

			clusterNode.setEpoch(Long.valueOf(resultLineArray[6]));

			clusterNode.setConnect("connected".equals(resultLineArray[7]));

			List<String> slots = Lists.newArrayList();
			for (int i = 8; i < resultLineArray.length; i++) {
				slots.add(resultLineArray[i]);
			}
			clusterNode.setServedSlots(StringUtils.join(slots, ","));

			clusterNodes.add(clusterNode);
		}
		return clusterNodes;
	}

	private ClusterInfo parseClusterInfoResult(String result) {
		ClusterInfo clusterInfo = new ClusterInfo();

		String[] line = StringUtils.split(result, "\n");
		for (String each : line) {
			String[] eachArray = StringUtils.split(each, ":");
			if (eachArray.length != 2) {
				continue;
			}

			String key = StringUtils.trim(eachArray[0]);
			String value = StringUtils.trim(eachArray[1]);

			switch (key) {
				case "cluster_state":
					clusterInfo.setOk("ok".equals(value));
					break;
				case "cluster_slots_assigned":
					clusterInfo.setSlotsAssigned(Integer.valueOf(value));
					break;
				case "cluster_slots_ok":
					clusterInfo.setSlotsOk(Integer.valueOf(value));
					break;
				case "cluster_slots_pfail":
					clusterInfo.setSlotsPfail(Integer.valueOf(value));
					break;
				case "cluster_slots_fail":
					clusterInfo.setSlotsFail(Integer.valueOf(value));
					break;
				case "cluster_known_nodes":
					clusterInfo.setKnownNodes(Integer.valueOf(value));
					break;
				case "cluster_size":
					clusterInfo.setSize(Integer.valueOf(value));
					break;
				case "cluster_current_epoch":
					clusterInfo.setCurrentEpoch(Integer.valueOf(value));
					break;
				case "cluster_stats_messages_sent":
					clusterInfo.setStatsMessagesSent(Long.valueOf(value));
					break;
				case "cluster_stats_messages_received":
					clusterInfo.setStatsMessagesReceived(Long.valueOf(value));
					break;
				default:
					break;
			}
		}

		return clusterInfo;
	}
}
