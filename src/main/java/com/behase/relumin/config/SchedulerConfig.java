package com.behase.relumin.config;

import lombok.Data;

@Data
public class SchedulerConfig {
	private long refreshClustersIntervalMillis;
	private long collectStaticsInfoIntervalMillis;
	private long collectStaticsInfoMaxCount;
}
