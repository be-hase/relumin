package com.behase.relumin.scheduler;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.fluentd.logger.FluentLogger;
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
import com.behase.relumin.model.Notice;
import com.behase.relumin.model.NoticeItem;
import com.behase.relumin.model.NoticeItem.NoticeOperator;
import com.behase.relumin.model.NoticeItem.NoticeType;
import com.behase.relumin.model.NoticeItem.NoticeValueType;
import com.behase.relumin.model.NoticeJob;
import com.behase.relumin.model.NoticeJob.ResultValue;
import com.behase.relumin.service.ClusterService;
import com.behase.relumin.service.NodeService;
import com.behase.relumin.service.NotifyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;
import redis.clients.util.Slowlog;

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
	NotifyService notifyService;

	@Autowired
	ObjectMapper mapper;

	@Autowired
	FluentLogger fluentLogger;

	@Value("${scheduler.collectStaticsInfoMaxCount:" + SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_MAX_COUNT + "}")
	private long collectStaticsInfoMaxCount;

	@Value("${redis.prefixKey}")
	private String redisPrefixKey;

	@Value("${notice.mail.host}")
	private String noticeMailHost;

	@Value("${notice.mail.port}")
	private int noticeMailPort;

	@Value("${notice.mail.from}")
	private String noticeMailFrom;

	@Value("${outputMetrics.fluentd.nodeTag}")
	private String outputMetricsFluentdNodeTag;

	@Scheduled(fixedDelayString = "${scheduler.collectStaticsInfoIntervalMillis:"
		+ SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_INTERVAL_MILLIS + "}")
	public void collectStaticsInfo() throws ApiException, IOException {
		log.info("collectStaticsInfo call");
		Set<String> clusterNames = clusterService.getClusters();
		for (String clusterName : clusterNames) {
			try {
				Notice notice = clusterService.getClusterNotice(clusterName);
				Cluster cluster = clusterService.getCluster(clusterName);
				List<ClusterNode> clusterNodes = cluster.getNodes();
				Map<ClusterNode, Map<String, String>> staticsInfos = Maps.newLinkedHashMap();

				for (ClusterNode clusterNode : clusterNodes) {
					try {
						log.info("collectStaticsInfo clusterName={}, hostAndPort={}", clusterName, clusterNode.getHostAndPort());
						Map<String, String> staticsInfo = nodeService.getStaticsInfo(clusterNode);
						staticsInfos.put(clusterNode, staticsInfo);

                        List<Slowlog> slowLogs = nodeService.getSlowLog(clusterNode);

						try (Jedis jedis = datastoreJedisPool.getResource()) {
							String key = Constants.getNodeStaticsInfoRedisKey(redisPrefixKey, clusterName, clusterNode.getNodeId());
							jedis.lpush(key, mapper.writeValueAsString(staticsInfo));
							jedis.ltrim(key, 0, collectStaticsInfoMaxCount - 1);

                            String slowLogKey = Constants.getNodeSlowLogRedisKey(redisPrefixKey, clusterName, clusterNode.getNodeId());
                            for (Slowlog slowLog : slowLogs) {
                                jedis.lpush(slowLogKey, mapper.writeValueAsString(slowLog));
                            }
						}   
					} catch (Exception e) {
						log.error("collectStaticsIndo fail. clusterName={}, hostAndPort={}", clusterName, clusterNode.getHostAndPort(), e);
					}
				}

				outputMetrics(cluster, staticsInfos);

				if (notice != null) {
					checkThresholdAndPublishNotify(notice, cluster, staticsInfos);
				}
			} catch (Exception e) {
				log.error("collectStaticsIndo fail. {}", clusterName, e);
			}
		}
		log.info("collectStaticsInfo finish");
	}

	public void outputMetrics(Cluster cluster, Map<ClusterNode, Map<String, String>> staticsInfos) {
		if (fluentLogger != null) {
			// node metrics
			staticsInfos.forEach((clusterNode, statics) -> {
				Map<String, Object> staticsObj = Maps.newHashMap();
				statics.forEach((k, v) -> {
					staticsObj.put(k, v);
				});
				staticsObj.put("node_id", clusterNode.getNodeId());
				staticsObj.put("host_and_port", clusterNode.getHostAndPort());
				log.debug("Logging on fluentd.");
				fluentLogger.log(
					String.format("%s", outputMetricsFluentdNodeTag, clusterNode.getNodeId()),
					staticsObj);
			});
		}
	}

	public void checkThresholdAndPublishNotify(Notice notice, Cluster cluster,
			Map<ClusterNode, Map<String, String>> staticsInfos) {
		log.debug("checkThresholdAndPublishNotify call. notice={}, cluster={}", notice, cluster);
		if (StringUtils.isNotBlank(notice.getInvalidEndTime())) {
			try {
				Long time = Long.valueOf(notice.getInvalidEndTime());
				if (System.currentTimeMillis() < time) {
					log.debug("NOW ignore to notify.");
					return;
				}
			} catch (Exception e) {
			}
		}
		String from = StringUtils.isNotBlank(notice.getMail().getFrom()) ? notice.getMail().getFrom() : noticeMailFrom;
		boolean notNotifyByMail = StringUtils.isBlank(noticeMailHost) || noticeMailPort == 0
			|| StringUtils.isBlank(from);
		boolean notNotifyByHttp = StringUtils.isBlank(notice.getHttp().getUrl());
		log.debug("{} {}", notNotifyByMail, notNotifyByHttp);
		if (notNotifyByMail && notNotifyByHttp) {
			if (notice.getItems().size() > 0) {
				log.warn("You set notification threshold, But mail or http is not set.");
			}
			return;
		}

		List<NoticeJob> noticeJobs = Lists.newArrayList();
		for (NoticeItem item : notice.getItems()) {
			switch (NoticeType.getNoticeType(item.getMetricsType())) {
				case CLUSTER_INFO:
					String targetClusterInfoVal;
					if ("cluster_state".equals(item.getMetricsName())) {
						targetClusterInfoVal = cluster.getStatus();
					} else {
						targetClusterInfoVal = cluster.getInfo().get(item.getMetricsName());
					}

					if (isNotify(item.getValueType(), item.getOperator(), targetClusterInfoVal, item.getValue())) {
						ResultValue result = new ResultValue("", "", targetClusterInfoVal);
						noticeJobs.add(new NoticeJob(item, Lists.newArrayList(result)));
					}
					break;
				case NODE_INFO:
					List<ResultValue> resultValues = Lists.newArrayList();
					for (Entry<ClusterNode, Map<String, String>> e : staticsInfos.entrySet()) {
						ClusterNode node = e.getKey();
						Map<String, String> staticsInfo = e.getValue();
						String targetNodeInfoVal = staticsInfo.get(item.getMetricsName());

						if (isNotify(item.getValueType(), item.getOperator(), targetNodeInfoVal, item.getValue())) {
							resultValues.add(new ResultValue(node.getNodeId(), node.getHostAndPort(), targetNodeInfoVal));
						}
					}
					if (resultValues.size() > 0) {
						noticeJobs.add(new NoticeJob(item, resultValues));
					}
					break;
				default:
					break;
			}
		}

		if (!noticeJobs.isEmpty()) {
			log.info("NOTIFY !! {}", noticeJobs);
			notifyService.notify(cluster, notice, noticeJobs);
		}

		log.debug("checkThresholdAndPublishNotify finish");
	}

	public boolean isNotify(String valueType, String operator, String value, String threshold) {
		if (value == null) {
			return false;
		}

		boolean isNotify = false;

		switch (NoticeValueType.getNoticeValueType(valueType)) {
			case STRING:
				switch (NoticeOperator.getNoticeOperator(operator)) {
					case EQ:
						isNotify = StringUtils.equalsIgnoreCase(value, threshold);
						break;
					case NE:
						isNotify = !StringUtils.equalsIgnoreCase(value, threshold);
						break;
					default:
						break;
				}
				break;
			case NUMBER:
				BigDecimal targetValNumber = new BigDecimal(value);
				int compareResult = targetValNumber.compareTo(new BigDecimal(threshold));
				switch (NoticeOperator.getNoticeOperator(operator)) {
					case EQ:
						isNotify = compareResult == 0;
						break;
					case NE:
						isNotify = compareResult != 0;
						break;
					case GT:
						isNotify = compareResult == 1;
						break;
					case GE:
						isNotify = compareResult == 1 || compareResult == 0;
						break;
					case LT:
						isNotify = compareResult == -1;
						break;
					case LE:
						isNotify = compareResult == -1 || compareResult == 0;
						break;
					default:
						break;
				}
				break;
			default:
				break;
		}

		return isNotify;
	}
}
