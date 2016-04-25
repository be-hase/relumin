package com.behase.relumin;

import com.google.common.base.Joiner;

public class Constants {
	private Constants() {
	};

	public static final int ALL_SLOTS_SIZE = 16384;

	public static final String ERR_CODE_INVALID_PARAMETER = "400_000";
	public static final String ERR_CODE_REDIS_SET_FAILED = "500_000";
	public static final String ERR_CODE_ALL_NODE_DOWN = "500_001";
	public static final String ERR_CODE_CLUSTER_NOT_AGREE_CONFIG = "500_002";
	public static final String ERR_CODE_CLUSTER_HAS_OPEN_SLOTS = "500_003";
	public static final String ERR_CODE_CLUSTER_HAS_ERRORS = "500_004";
	public static final String ERR_CODE_UNKNOWN = "500_999";

	public static String getUsersRedisKey(String prefixKey) {
		return Joiner.on(".").join(prefixKey, "users").toString();
	}

	public static String getClustersRedisKey(String prefixKey) {
		return Joiner.on(".").join(prefixKey, "clusters").toString();
	}

	public static String getClusterRedisKey(String prefixKey, String clusterName) {
		return Joiner.on(".").join(prefixKey, "cluster", clusterName).toString();
	}

	public static String getClusterNoticeRedisKey(String prefixKey, String clusterName) {
		return Joiner.on(".").join(prefixKey, "cluster", clusterName, "notice").toString();
	}

	public static String getNodeRedisKey(String prefixKey, String clusterName, String nodeId) {
		return Joiner.on(".").join(prefixKey, "cluster", clusterName, "node", nodeId).toString();
	}

	public static String getNodeStaticsInfoRedisKey(String prefixKey, String clusterName, String nodeId) {
		return Joiner.on(".").join(prefixKey, "cluster", clusterName, "node", nodeId, "staticsInfo").toString();
	}

	public static String getNodeSlowLogRedisKey(String prefixKey, String clusterName, String nodeId) {
		return Joiner.on(".").join(prefixKey, "cluster", clusterName, "node", nodeId, "slowLog").toString();
	}
}
