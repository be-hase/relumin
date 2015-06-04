package com.behase.relumin.service;

import java.io.IOException;
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
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.model.SlotInfo;
import com.behase.relumin.util.JedisUtils;
import com.behase.relumin.util.ValidationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClusterServiceImpl implements ClusterService {
	@Autowired
	JedisPool dataStoreJedisPool;

	@Autowired
	ObjectMapper mapper;

	@Override
	public Set<String> getClusters() {
		try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			return dataStoreJedis.smembers(Constants.getClustersKey());
		}
	}

	@Override
	public Cluster getCluster(String clusterName) throws IOException {
		ClusterNode node = getActiveClusterNode(clusterName);
		Cluster cluster = getClusterByHostAndPort(node.getHostAndPort());
		cluster.setClusterName(clusterName);
		return cluster;
	}

	@Override
	public Cluster getClusterByHostAndPort(String hostAndPort) throws IOException {
		Cluster cluster = new Cluster();

		try (Jedis jedis = JedisUtils.getJedisByHostAndPort(hostAndPort)) {
			// info
			Map<String, String> info = JedisUtils.parseClusterInfoResult(jedis.clusterInfo());

			// nodes
			List<ClusterNode> nodes = JedisUtils.parseClusterNodesResult(jedis.clusterNodes(), hostAndPort);
			nodes.sort((o1, o2) -> {
				if (StringUtils.equals(o1.getHost(), o2.getHost())) {
					return Integer.compare(o1.getPort(), o2.getPort());
				} else {
					return o1.getHost().compareTo(o2.getHost());
				}
			});

			// slots
			List<SlotInfo> slots = Lists.newArrayList();
			Map<String, List<Map<String, String>>> replicaRelations = Maps.newHashMap();
			nodes.forEach(v -> {
				Map<String, String> nodeMap = Maps.newLinkedHashMap();
				nodeMap.put("node_id", v.getNodeId());
				nodeMap.put("host_and_port", v.getHostAndPort());

				boolean isMaster = StringUtils.isNotBlank(v.getServedSlots());
				if (isMaster) {
					String servedSlots = v.getServedSlots();
					String[] slotsRangesArray = StringUtils.split(servedSlots, ",");
					for (String slotsRangeStr : slotsRangesArray) {
						SlotInfo slotInfo = new SlotInfo();
						slotInfo.setMaster(nodeMap);
						if (StringUtils.indexOf(slotsRangeStr, "-") == StringUtils.INDEX_NOT_FOUND) {
							slotInfo.setStartSlotNumber(Integer.valueOf(slotsRangeStr));
							slotInfo.setEndSlotNumber(Integer.valueOf(slotsRangeStr));
						} else {
							String[] slotsRange = StringUtils.split(slotsRangeStr, "-");
							slotInfo.setStartSlotNumber(Integer.valueOf(slotsRange[0]));
							slotInfo.setEndSlotNumber(Integer.valueOf(slotsRange[1]));
						}
						slots.add(slotInfo);
					}
				} else {
					List<Map<String, String>> replicas = replicaRelations.get(v.getMasterNodeId());
					if (replicas == null) {
						replicas = Lists.newArrayList();
						replicaRelations.put(v.getMasterNodeId(), replicas);
					}
					replicas.add(nodeMap);
				}
			});
			slots.forEach(v -> {
				List<Map<String, String>> replicas = replicaRelations.get(v.getMaster().get("node_id"));
				v.setReplicas(replicas);
			});
			slots.sort((o1, o2) -> Integer.compare(o1.getStartSlotNumber(), o2.getStartSlotNumber()));

			cluster.setInfo(info);
			cluster.setNodes(nodes);
			cluster.setSlots(slots);

			return cluster;
		}
	}

	@Override
	public boolean existsClusterName(String clusterName) {
		try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			Set<String> clusterNames = dataStoreJedis.smembers(Constants.getClustersKey());
			return clusterNames.contains(clusterName);
		}
	}

	@Override
	public void setCluster(String clusterName, String hostAndPort) throws JsonProcessingException {
		ValidationUtils.clusterName(clusterName);
		ValidationUtils.hostAndPort(hostAndPort);

		try (
				Jedis jedis = JedisUtils.getJedisByHostAndPort(hostAndPort);
				Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			try {
				jedis.ping();
			} catch (Exception e) {
				log.warn("redis error.", e);
				throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, String.format("Failed to connect to Redis Cluster(%s). Please confirm.", hostAndPort), HttpStatus.BAD_REQUEST);
			}

			Map<String, String> info = JedisUtils.parseInfoResult(jedis.info());
			log.debug("cluster info={}", info);
			String clusterEnabled = info.get("cluster_enabled");
			if (StringUtils.isBlank(clusterEnabled) || StringUtils.equals(clusterEnabled, "0")) {
				throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, String.format("This Redis(%s) is not cluster mode.", hostAndPort), HttpStatus.BAD_REQUEST);
			}

			List<ClusterNode> nodes = JedisUtils.parseClusterNodesResult(jedis.clusterNodes(), hostAndPort);

			dataStoreJedis.watch(Constants.getClustersKey(), Constants.getClusterKey(clusterName));
			Transaction t = dataStoreJedis.multi();
			t.sadd(Constants.getClustersKey(), clusterName);
			t.set(Constants.getClusterKey(clusterName), mapper.writeValueAsString(nodes));
			t.exec();
		}
	}

	@Override
	public void deleteCluster(String clusterName) {
		try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			Set<String> keys = dataStoreJedis.keys(Constants.getClusterKey(clusterName) + ".*");

			dataStoreJedis.watch(Constants.getClustersKey(), Constants.getClusterKey(clusterName));
			Transaction t = dataStoreJedis.multi();
			t.srem(Constants.getClustersKey(), clusterName);
			t.del(Constants.getClusterKey(clusterName));
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

	private ClusterNode getActiveClusterNode(String clusterName) throws IOException {
		try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			String result = dataStoreJedis.get(Constants.getClusterKey(clusterName));
			if (StringUtils.isBlank(result)) {
				throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Not exists this cluster name.", HttpStatus.BAD_REQUEST);
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

			throw new ApiException(Constants.ERR_CODE_ALL_NODE_DOWN, "All node is down.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
