package com.behase.relumin.service;

import java.util.Map;

import com.behase.relumin.model.ClusterNode;

public interface NodeService {
	Map<String, Object> getStaticsInfo(ClusterNode clusterNode);
}
