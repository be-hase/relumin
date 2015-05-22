package com.behase.relumin.scheduler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.behase.relumin.Constants;
import com.behase.relumin.config.SchedulerConfig;
import com.behase.relumin.exception.ApiException;
import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.service.ClusterService;
import com.behase.relumin.service.NodeService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile(value = "!test")
public class NodeScheduler {
	@Autowired
	ClusterService clusterService;

	@Autowired
	NodeService nodeService;

	@Autowired
	JedisPool datastoreJedisPool;

	@Autowired
	ObjectMapper mapper;

	@Value("${scheduler.collectStaticsInfoMaxCount:" + SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_MAX_COUNT + "}")
	private long collectStaticsInfoMaxCount;

	@Scheduled(fixedDelayString = "${scheduler.collectStaticsInfoIntervalMillis:"
		+ SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_INTERVAL_MILLIS + "}")
	public void collectStaticsInfo() throws ApiException, IOException {
		log.info("collectStaticsIndo call");
		Set<String> clusterNames = clusterService.getClusters();
		for (String clusterName : clusterNames) {
			try {
				Cluster cluster = clusterService.getCluster(clusterName);
				List<ClusterNode> clusterNodes = cluster.getNodes();

				for (ClusterNode clusterNode : clusterNodes) {
					try {
						Map<String, String> staticsInfo = nodeService.getStaticsInfo(clusterNode);
						log.info("staticsInfo : {}", staticsInfo);

						try (Jedis jedis = datastoreJedisPool.getResource()) {
							String key = Constants.getNodeStaticsInfoKey(clusterName, clusterNode.getNodeId());
							jedis.lpush(key, mapper.writeValueAsString(staticsInfo));
							jedis.ltrim(key, 0, collectStaticsInfoMaxCount - 1);
						}
					} catch (Exception e) {
						log.error("collectStaticsIndo fail. {}, {}", clusterName, clusterNode.getHostAndPort(), e);
					}
				}
			} catch (Exception e) {
				log.error("collectStaticsIndo fail. {}", clusterName, e);
			}
		}
		log.info("collectStaticsIndo finish");
	}
}
