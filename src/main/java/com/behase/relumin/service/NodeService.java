package com.behase.relumin.service;

import java.util.List;
import java.util.Map;

import com.behase.relumin.model.ClusterNode;

public interface NodeService {
	Map<String, Object> getStaticsInfo(ClusterNode clusterNode);

	List<Map<String, Object>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end);

	List<Map<String, Object>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			List<String> fileds);

	List<Map<String, Object>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			boolean isTimeAsc);

	List<Map<String, Object>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			List<String> fileds, boolean isTimeAsc);
}
