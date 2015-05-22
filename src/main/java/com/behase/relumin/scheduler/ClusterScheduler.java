package com.behase.relumin.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.behase.relumin.config.SchedulerConfig;
import com.behase.relumin.service.ClusterService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile(value = "!test")
public class ClusterScheduler {
	@Autowired
	ClusterService clusterService;

	@Scheduled(fixedDelayString = "${scheduler.refreshClustersIntervalMillis:"
		+ SchedulerConfig.DEFAULT_REFRESH_CLUSTERS_INTERVAL_MILLIS + "}"
			)
			public void refreshClusters() {
		log.info("refreshClusters call");
		clusterService.refreshClusters();
		log.info("refreshClusters finish");
	}
}
