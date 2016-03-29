package com.behase.relumin.service;

import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.support.JedisSupport;
import com.behase.relumin.support.RedisTrib;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
public class RedisTribServiceImpl implements RedisTribService {
    @Autowired
    private JedisSupport jedisSupport;

    @Override
    public List<CreateClusterParam> getCreateClusterParams(int replicas, List<String> hostAndPortRanges)
            throws IOException {
        Set<String> hostAndPorts = jedisSupport.getHostAndPorts(hostAndPortRanges);

        try (RedisTrib trib = new RedisTrib()) {
            return trib.getCreateClusterParams(replicas, hostAndPorts);
        }
    }

    @Override
    public void createCluster(List<CreateClusterParam> params) throws Exception {
        try (RedisTrib trib = createRedisTrib()) {
            trib.createCluster(params);
        }
    }

    @Override
    public void reshardCluster(String hostAndPort, int slotCount, String fromNodeIds, String toNodeId) throws Exception {
        try (RedisTrib trib = createRedisTrib()) {
            trib.reshardCluster(hostAndPort, slotCount, fromNodeIds, toNodeId);
        }
    }

    @Override
    public void reshardClusterBySlots(String hostAndPort, List<String> slotsStr, String toNodeId) throws Exception {
        Set<Integer> slots = jedisSupport.getSlots(slotsStr);

        try (RedisTrib trib = createRedisTrib()) {
            trib.reshardClusterBySlots(hostAndPort, slots, toNodeId);
        }
    }

    @Override
    public List<String> checkCluster(String hostAndPort) throws Exception {
        try (RedisTrib trib = createRedisTrib()) {
            return trib.checkCluster(hostAndPort);
        }
    }

    @Override
    public void fixCluster(String hostAndPort) throws Exception {
        try (RedisTrib trib = createRedisTrib()) {
            trib.fixCluster(hostAndPort);
        }
    }

    @Override
    public void addNodeIntoCluster(String hostAndPort, String newHostAndPort, String masterNodeId) throws Exception {
        try (RedisTrib trib = createRedisTrib()) {
            if (StringUtils.isBlank(masterNodeId)) {
                trib.addNodeIntoCluster(hostAndPort, newHostAndPort);
            } else {
                trib.addNodeIntoClusterAsReplica(hostAndPort, newHostAndPort, masterNodeId);
            }
        }
    }

    @Override
    public void deleteNodeFromCluster(String hostAndPort, String nodeId, String reset, boolean shutdown)
            throws Exception {
        try (RedisTrib trib = createRedisTrib()) {
            trib.deleteNodeOfCluster(hostAndPort, nodeId, reset, shutdown);
        }
    }

    @Override
    public void deleteFailNodeFromCluster(String hostAndPort, String nodeId)
            throws Exception {
        try (RedisTrib trib = createRedisTrib()) {
            trib.deleteFailNodeOfCluster(hostAndPort, nodeId);
        }
    }

    @Override
    public void replicateNode(String hostAndPort, String masterNodeId) throws Exception {
        try (RedisTrib trib = createRedisTrib()) {
            trib.replicateNode(hostAndPort, masterNodeId);
        }
    }

    @Override
    public void failoverNode(String hostAndPort) throws Exception {
        try (RedisTrib trib = createRedisTrib()) {
            trib.failoverNode(hostAndPort);
        }
    }

    RedisTrib createRedisTrib() {
        return new RedisTrib();
    }
}
