package com.behase.relumin.service;

import com.behase.relumin.Constants;
import com.behase.relumin.exception.ApiException;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.model.Notice;
import com.behase.relumin.model.SlotInfo;
import com.behase.relumin.support.JedisSupport;
import com.behase.relumin.util.ValidationUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ClusterServiceImpl implements ClusterService {
    @Autowired
    private JedisPool dataStoreJedisPool;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private JedisSupport jedisSupport;

    @Value("${redis.prefixKey}")
    private String redisPrefixKey;

    @Override
    public Set<String> getClusters() {
        try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
            return dataStoreJedis.smembers(Constants.getClustersRedisKey(redisPrefixKey));
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

        try (Jedis jedis = jedisSupport.getJedisByHostAndPort(hostAndPort)) {
            // info
            Map<String, String> info = jedisSupport.parseClusterInfoResult(jedis.clusterInfo());

            // nodes
            List<ClusterNode> nodes = jedisSupport.parseClusterNodesResult(jedis.clusterNodes(), hostAndPort);
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
                } else { // slave
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
            Set<String> clusterNames = dataStoreJedis.smembers(Constants.getClustersRedisKey(redisPrefixKey));
            return clusterNames.contains(clusterName);
        }
    }

    @Override
    public void setCluster(String clusterName, String hostAndPort) throws JsonProcessingException {
        ValidationUtils.clusterName(clusterName);
        ValidationUtils.hostAndPort(hostAndPort);

        try (
                Jedis jedis = jedisSupport.getJedisByHostAndPort(hostAndPort);
                Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
            try {
                jedis.ping();
            } catch (Exception e) {
                log.warn("Redis error.", e);
                throw new InvalidParameterException(String.format("Failed to connect to Redis Cluster(%s). Please confirm.", hostAndPort));
            }

            Map<String, String> info = jedisSupport.parseInfoResult(jedis.info());
            log.debug("Cluster info={}", info);
            String clusterEnabled = info.get("cluster_enabled");
            if (StringUtils.isBlank(clusterEnabled) || StringUtils.equals(clusterEnabled, "0")) {
                throw new InvalidParameterException(String.format("This Redis(%s) is not cluster mode.", hostAndPort));
            }

            List<ClusterNode> nodes = jedisSupport.parseClusterNodesResult(jedis.clusterNodes(), hostAndPort);

            Pipeline p = dataStoreJedis.pipelined();
            p.sadd(Constants.getClustersRedisKey(redisPrefixKey), clusterName);
            p.set(Constants.getClusterRedisKey(redisPrefixKey, clusterName), mapper.writeValueAsString(nodes));
            p.sync();
        }
    }

    @Override
    public void changeClusterName(String clusterName, String newClusterName) throws IOException {
        ValidationUtils.clusterName(newClusterName);
        if (!existsClusterName(clusterName)) {
            throw new InvalidParameterException(String.format("Cluster name(%s) does not exists. Please confirm.", clusterName));
        }
        if (existsClusterName(newClusterName)) {
            throw new InvalidParameterException(String.format("New cluster name(%s) already exists. Please confirm.", newClusterName));
        }

        String clusterPrefixKey = Constants.getClusterRedisKey(redisPrefixKey, clusterName);
        try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
            Set<String> currentKeys = Sets.newHashSet(clusterPrefixKey);
            currentKeys.addAll(dataStoreJedis.keys(clusterPrefixKey + ".*"));

            Pipeline p = dataStoreJedis.pipelined();
            currentKeys.forEach(currentKey -> {
                String newKey = Constants.getClusterRedisKey(redisPrefixKey, newClusterName)
                        + StringUtils.removeStart(currentKey, clusterPrefixKey);
                p.rename(currentKey, newKey);
            });
            p.sadd(Constants.getClustersRedisKey(redisPrefixKey), newClusterName);
            p.srem(Constants.getClustersRedisKey(redisPrefixKey), clusterName);
            p.sync();
        }
    }

    @Override
    public Notice getClusterNotice(String clusterName) throws IOException {
        try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
            String result = dataStoreJedis.get(Constants.getClusterNoticeRedisKey(redisPrefixKey, clusterName));
            if (StringUtils.isBlank(result)) {
                return null;
            }
            return mapper.readValue(result, Notice.class);
        }
    }

    @Override
    public void setClusterNotice(String clusterName, Notice notice) throws IOException {
        try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
            String json = mapper.writeValueAsString(notice);
            dataStoreJedis.set(Constants.getClusterNoticeRedisKey(redisPrefixKey, clusterName), json);
        }
    }

    @Override
    public void deleteCluster(String clusterName) {
        try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
            Set<String> keys = dataStoreJedis.keys(Constants.getClusterRedisKey(redisPrefixKey, clusterName) + ".*");

            Pipeline p = dataStoreJedis.pipelined();
            p.srem(Constants.getClustersRedisKey(redisPrefixKey), clusterName);
            p.del(Constants.getClusterRedisKey(redisPrefixKey, clusterName));
            p.del(Constants.getClusterNoticeRedisKey(redisPrefixKey, clusterName));
            if (keys.size() > 0) {
                p.del(keys.toArray(new String[keys.size()]));
            }
            p.sync();
        }
    }

    @Override
    public void refreshClusters() {
        log.debug("refresh start");
        Set<String> clusters = getClusters();
        for (String clusterName : clusters) {
            log.debug("refresh cluster. clusterName : {}", clusterName);
            try {
                ClusterNode clusterNode = getActiveClusterNode(clusterName);

                try (
                        Jedis jedis = jedisSupport.getJedisByHostAndPort(clusterNode.getHostAndPort());
                        Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {

                    // GET
                    List<ClusterNode> nodes = jedisSupport.parseClusterNodesResult(jedis.clusterNodes(), clusterNode.getHostAndPort());

                    // update
                    dataStoreJedis.set(Constants.getClusterRedisKey(redisPrefixKey, clusterName), mapper.writeValueAsString(nodes));

                    // remove deleted(node) statics
                    Set<String> keys = dataStoreJedis.keys(Constants.getClusterRedisKey(redisPrefixKey, clusterName)
                            + ".*.staticsInfo");
                    Set<String> existOnRedisNodeIds = keys.stream().map(key -> {
                        String nodeId = StringUtils.removeStart(key, Constants.getClusterRedisKey(redisPrefixKey, clusterName)
                                + ".node.");
                        nodeId = StringUtils.removeEnd(nodeId, ".staticsInfo");
                        return nodeId;
                    }).collect(Collectors.toSet());
                    log.debug("existOnRedisNodeIds : {}", existOnRedisNodeIds);

                    Set<String> realNodeIds = nodes.stream().map(node -> node.getNodeId()).collect(Collectors.toSet());
                    log.debug("realNodeIds : {}", realNodeIds);

                    existOnRedisNodeIds.removeAll(realNodeIds);
                    log.debug("extra NodeIds : {}", existOnRedisNodeIds);
                    if (!existOnRedisNodeIds.isEmpty()) {
                        for (String nodeId : existOnRedisNodeIds) {
                            String key = Constants.getNodeStaticsInfoRedisKey(redisPrefixKey, clusterName, nodeId);
                            log.info("delete extra data node. key : {}", key);
                            dataStoreJedis.del(key);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("refresh fail. {}", clusterName, e);
            }
        }
        log.debug("refresh end");
    }

    @Override
    public ClusterNode getActiveClusterNode(String clusterName) throws IOException {
        return getActiveClusterNodeWithExcludePredicate(clusterName, null);
    }

    @Override
    public ClusterNode getActiveClusterNodeWithExcludeNodeId(String clusterName, String nodeId) throws IOException {
        return getActiveClusterNodeWithExcludePredicate(
                clusterName,
                clusterNode -> StringUtils.equalsIgnoreCase(clusterNode.getNodeId(), nodeId)
        );
    }

    @Override
    public ClusterNode getActiveClusterNodeWithExcludeHostAndPort(String clusterName, String hostAndPort)
            throws IOException {
        return getActiveClusterNodeWithExcludePredicate(
                clusterName,
                clusterNode -> StringUtils.equalsIgnoreCase(clusterNode.getHostAndPort(), hostAndPort)
        );
    }

    ClusterNode getActiveClusterNodeWithExcludePredicate(String clusterName, Predicate<ClusterNode> excludePredicate)
            throws IOException {
        try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
            String result = dataStoreJedis.get(Constants.getClusterRedisKey(redisPrefixKey, clusterName));
            if (StringUtils.isBlank(result)) {
                throw new InvalidParameterException("Not exists this cluster name.");
            }

            List<ClusterNode> existClusterNodes = mapper.readValue(result, new TypeReference<List<ClusterNode>>() {
            });
            Collections.shuffle(existClusterNodes);

            for (ClusterNode clusterNode : existClusterNodes) {
                if (excludePredicate != null && excludePredicate.test(clusterNode)) {
                    continue;
                }
                try (Jedis jedis = jedisSupport.getJedisByHostAndPort(clusterNode.getHostAndPort())) {
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


    @Override
    public Map<String, Map<String, List<List<Object>>>> getClusterStaticsInfoHistory(String clusterName,
                                                                                     List<String> nodes,
                                                                                     List<String> fields, long start, long end) {
        if (end < start) {
            throw new InvalidParameterException("End time must be larger than start time.");
        }

        Map<String, Map<String, List<List<Object>>>> result = Maps.newLinkedHashMap();

        nodes.parallelStream().forEach(nodeId -> {
            log.debug("node loop : {}", nodeId);
            Map<String, List<List<Object>>> staticsInfoHistory = nodeService.getStaticsInfoHistory(clusterName, nodeId, fields, start, end);
            if (!staticsInfoHistory.isEmpty()) {
                result.put(nodeId, staticsInfoHistory);
            }
        });

        return result;
    }
}
