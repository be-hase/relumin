package com.behase.relumin.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.behase.relumin.service.ClusterService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ClusterScheduler {
	@Autowired
	ClusterService clusterService;

	@Scheduled(fixedDelay = 5 * 60 * 1000)
	public void refreshClusters() {
		log.info("refreshClusters call");
		clusterService.refreshClusters();
		log.info("refreshClusters finish");
	}
}
