package com.behase.relumin.service;

import com.behase.relumin.model.param.CreateClusterParam;

import java.io.IOException;
import java.util.List;

public interface RedisTribService {
    List<CreateClusterParam> getCreateClusterParams(int replicas, List<String> hostAndPorts) throws IOException;

    void createCluster(List<CreateClusterParam> params) throws Exception;

    void reshardCluster(String hostAndPort, int slotCount, String fromNodeIds, String toNodeId) throws Exception;

    void reshardClusterBySlots(String hostAndPort, List<String> slots, String toNodeId) throws Exception;

    List<String> checkCluster(String hostAndPort) throws Exception;

    void fixCluster(String hostAndPort) throws Exception;

    void addNodeIntoCluster(String hostAndPort, String newHostAndPort, String masterNodeId)
            throws Exception;

    void deleteNodeFromCluster(String hostAndPort, String nodeId, String reset, boolean shutdown) throws Exception;

    void deleteFailNodeFromCluster(String hostAndPort, String nodeId) throws Exception;

    void replicateNode(String hostAndPort, String masterNodeId) throws Exception;

    void failoverNode(String hostAndPort) throws Exception;
}
