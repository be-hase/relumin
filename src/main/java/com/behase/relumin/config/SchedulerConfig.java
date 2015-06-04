package com.behase.relumin.config;

import lombok.Data;

@Data
public class SchedulerConfig {
	public static final String DEFAULT_REFRESH_CLUSTERS_INTERVAL_MILLIS = "12000000";
	public static final String DEFAULT_COLLECT_STATICS_INFO_INTERVAL_MILLIS = "12000000";
	public static final String DEFAULT_COLLECT_STATICS_INFO_MAX_COUNT = "1500";

	private String refreshClustersIntervalMillis;
	private String collectStaticsInfoIntervalMillis;
	private String collectStaticsInfoMaxCount;
}
