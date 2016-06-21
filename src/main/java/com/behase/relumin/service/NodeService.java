package com.behase.relumin.service;

import com.behase.relumin.model.ClusterNode;

import java.util.List;
import java.util.Map;

public interface NodeService {
    Map<String, String> getStaticsInfo(ClusterNode clusterNode);

    Map<String, List<List<Object>>> getStaticsInfoHistory(String clusterName, String nodeId, List<String> fields,
                                                          long start, long end);

    void shutdown(String hostAndPort);
}
