package com.behase.relumin.service;

import java.util.List;
import java.util.Map;

import com.behase.relumin.model.ClusterNode;

public interface NodeService {
	Map<String, String> getStaticsInfo(ClusterNode clusterNode);

	List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end);

	List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			List<String> fileds);

	List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			boolean isTimeAsc);

	List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			List<String> fileds, boolean isTimeAsc);
}
