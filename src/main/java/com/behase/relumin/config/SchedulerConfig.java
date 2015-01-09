package com.behase.relumin.config;

import lombok.Data;

@Data
public class SchedulerConfig {
	private long refreshClustersIntervalMill;
	private long collectStaticsInfoIntervalMill;
}
