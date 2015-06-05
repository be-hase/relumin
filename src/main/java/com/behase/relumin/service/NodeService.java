package com.behase.relumin.service;

import java.util.List;
import java.util.Map;

import com.behase.relumin.model.ClusterNode;

public interface NodeService {
	Map<String, String> getStaticsInfo(ClusterNode clusterNode);

	List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, List<String> fields,
			long start, long end);

	List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, List<String> fields,
			long start, long end, boolean isTimeAsc);

	void shutdown(String hostAndPort);
}
