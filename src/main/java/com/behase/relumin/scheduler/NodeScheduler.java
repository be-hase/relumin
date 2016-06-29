package com.behase.relumin.scheduler;

import com.behase.relumin.Constants;
import com.behase.relumin.config.SchedulerConfig;
import com.behase.relumin.exception.ApiException;
import com.behase.relumin.model.*;
import com.behase.relumin.model.NoticeItem.NoticeOperator;
import com.behase.relumin.model.NoticeItem.NoticeType;
import com.behase.relumin.model.NoticeItem.NoticeValueType;
import com.behase.relumin.model.NoticeJob.ResultValue;
import com.behase.relumin.service.ClusterService;
import com.behase.relumin.service.NodeService;
import com.behase.relumin.service.NotifyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fluentd.logger.FluentLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Profile(value = "!test")
public class NodeScheduler {
    @Autowired
    private ClusterService clusterService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private JedisPool datastoreJedisPool;

    @Autowired
    private NotifyService notifyService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private FluentLogger fluentLogger;

    @Value("${scheduler.collectStaticsInfoMaxCount:" + SchedulerConfig.DEFAULT_COLLECT_STATICS_INFO_MAX_COUNT +
            "}")
    private long collectStaticsInfoMaxCount;

    @Value("${scheduler.collectSlowLogMaxCount:" + SchedulerConfig.DEFAULT_COLLECT_SLOW_LOG_MAX_COUNT +
            "}")
    private long collectSlowLogMaxCount;

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
        clusterNames.parallelStream().forEach(clusterName -> {
            try {
                Notice notice = clusterService.getClusterNotice(clusterName);
                Cluster cluster = clusterService.getCluster(clusterName);
                List<ClusterNode> clusterNodes = cluster.getNodes();
                Map<ClusterNode, Map<String, String>> staticsInfos = Maps.newConcurrentMap();
                List<SlowLog> slowLogs = Collections.synchronizedList(Lists.newArrayList());

                // collect statics and slowLog
                clusterNodes.parallelStream().forEach(clusterNode -> {
                    try {
                        Map<String, String> staticsInfo = nodeService.getStaticsInfo(clusterNode);
                        staticsInfos.put(clusterNode, staticsInfo);

                        if (collectSlowLogMaxCount > 0) {
                            slowLogs.addAll(nodeService.getSlowLog(clusterNode));
                        }

                        try (Jedis jedis = datastoreJedisPool.getResource()) {
                            log.debug("Save staticsInfo to redis.");
                            String key = Constants.getNodeStaticsInfoRedisKey(redisPrefixKey, clusterName,
                                    clusterNode.getNodeId());
                            jedis.lpush(key, mapper.writeValueAsString(staticsInfo));
                            jedis.ltrim(key, 0, collectStaticsInfoMaxCount - 1);
                        }
                    } catch (Exception e) {
                        log.error("collectStaticsInfo fail. clusterName={}, hostAndPort={}", clusterName,
                                clusterNode.getHostAndPort(), e);
                    }
                });

                // sort slowLog, and save
                try {
                    saveSlowLogs(slowLogs, clusterName);
                } catch (Exception e) {
                    log.error("saveSlowLogs fail. clusterName={}", clusterName, e);
                }

                // Output metrics
                try {
                    outputMetrics(cluster, staticsInfos);
                } catch (Exception e) {
                    log.error("outputMetrics fail. clusterName={}", clusterName, e);
                }

                // Notice
                if (notice == null) {
                    log.debug("No notice setting, so skip.");
                } else {
                    List<NoticeJob> noticeJobs = getNoticeJobs(notice, cluster, staticsInfos);
                    if (!noticeJobs.isEmpty()) {
                        log.info("NOTIFY !! {}", noticeJobs);
                        notifyService.notify(cluster, notice, noticeJobs);
                    }
                }
            } catch (Exception e) {
                log.error("collectStaticsInfo fail. {}", clusterName, e);
            }
        });
        log.info("collectStaticsInfo finish");
    }

    void saveSlowLogs(List<SlowLog> slowLogs, String clusterName) {
        try (Jedis jedis = datastoreJedisPool.getResource()) {
            String key = Constants.getClusterSlowLogRedisKey(redisPrefixKey, clusterName);

            PagerData<SlowLog> mostRecentPd = clusterService.getClusterSlowLogHistory(clusterName, 0, 0);
            final long mostRecent = mostRecentPd.getData().isEmpty() ? 0L : mostRecentPd.getData().get(0).getTimeStamp();

            // filter by timestamp
            slowLogs = slowLogs.stream().filter(v -> v.getTimeStamp() > mostRecent).collect(Collectors.toList());

            if (slowLogs.size() == 0) {
                return;
            }

            // sort by timestamp
            slowLogs.sort((i, k) -> Long.compare(i.getTimeStamp(), k.getTimeStamp()));

            List<String> slowLogStrList = slowLogs.stream().map(v -> {
                try {
                    return mapper.writeValueAsString(v);
                } catch (JsonProcessingException ignore) {
                    return null;
                }
            }).filter(v -> v != null).collect(Collectors.toList());

            if (slowLogStrList.size() > 0) {
                jedis.lpush(key, slowLogStrList.toArray(new String[slowLogStrList.size()]));
                jedis.ltrim(key, 0, collectSlowLogMaxCount - 1);
            }
        }
    }

    void outputMetrics(Cluster cluster, Map<ClusterNode, Map<String, String>> staticsInfos) {
        if (fluentLogger != null) {
            // node metrics
            staticsInfos.forEach((clusterNode, statics) -> {
                Map<String, Object> staticsObj = Maps.newHashMap();
                statics.forEach((k, v) -> staticsObj.put(k, v));
                staticsObj.put("cluster_name", cluster.getClusterName());
                staticsObj.put("node_id", clusterNode.getNodeId());
                staticsObj.put("host_and_port", clusterNode.getHostAndPort());
                log.debug("Logging on fluentd.");
                fluentLogger.log(
                        String.format("%s.%s", outputMetricsFluentdNodeTag, clusterNode.getNodeId()),
                        staticsObj);
            });
        }
    }

    List<NoticeJob> getNoticeJobs(Notice notice, Cluster cluster,
                                  Map<ClusterNode, Map<String, String>> staticsInfos) {
        List<NoticeJob> noticeJobs = Lists.newArrayList();

        if (isInInvalidEndTime(notice)) {
            // ignore
            return noticeJobs;
        }

        for (NoticeItem item : notice.getItems()) {
            switch (NoticeType.getNoticeType(item.getMetricsType())) {
                case CLUSTER_INFO:
                    String targetClusterInfoVal;
                    if ("cluster_state".equals(item.getMetricsName())) {
                        targetClusterInfoVal = cluster.getStatus();
                    } else {
                        targetClusterInfoVal = cluster.getInfo().get(item.getMetricsName());
                    }

                    if (shouldNotify(item.getValueType(), item.getOperator(), targetClusterInfoVal, item.getValue())) {
                        ResultValue result = new ResultValue("", "", targetClusterInfoVal);
                        noticeJobs.add(new NoticeJob(item, Lists.newArrayList(result)));
                    }
                    break;
                case NODE_INFO:
                    List<ResultValue> resultValues = Lists.newArrayList();

                    staticsInfos.forEach((node, staticsInfo) -> {
                        String targetNodeInfoVal = staticsInfo.get(item.getMetricsName());

                        if (shouldNotify(item.getValueType(), item.getOperator(), targetNodeInfoVal, item.getValue())) {
                            resultValues.add(
                                    new ResultValue(node.getNodeId(), node.getHostAndPort(), targetNodeInfoVal));
                        }
                    });

                    if (resultValues.size() > 0) {
                        noticeJobs.add(new NoticeJob(item, resultValues));
                    }
                    break;
                default:
                    break;
            }
        }

        return noticeJobs;
    }

    boolean isInInvalidEndTime(Notice notice) {
        if (StringUtils.isNotBlank(notice.getInvalidEndTime())) {
            try {
                Long time = Long.valueOf(notice.getInvalidEndTime());
                if (System.currentTimeMillis() < time) {
                    log.info("NOW ignore notify. notice={}", notice);
                    return true;
                }
            } catch (Exception e) {
            }
        }

        return false;
    }

    boolean shouldNotify(String valueType, String operator, String value, String threshold) {
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
