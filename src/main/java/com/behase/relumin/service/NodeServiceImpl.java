package com.behase.relumin.service;

import com.behase.relumin.Constants;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.model.SlowLog;
import com.behase.relumin.support.JedisSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Slowlog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NodeServiceImpl implements NodeService {
    private static final int OFFSET = 999;

    @Autowired
    private JedisPool dataStoreJedisPool;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JedisSupport jedisSupport;

    @Value("${redis.prefixKey}")
    private String redisPrefixKey;

    @Override
    public Map<String, String> getStaticsInfo(ClusterNode clusterNode) {
        try (Jedis jedis = jedisSupport.getJedisByHostAndPort(clusterNode.getHostAndPort())) {
            return jedisSupport.parseInfoResult(jedis.info());
        }
    }

    @Override
    public Map<String, List<List<Object>>> getStaticsInfoHistory(String clusterName, String nodeId,
                                                                 List<String> fields,
                                                                 long start, long end) {
        if (end < start) {
            throw new InvalidParameterException("End time must be larger than start time.");
        }

        List<Map<String, String>> rangeResult = Lists.newArrayList();
        int startIndex = 0;
        int endIndex = OFFSET;
        boolean validStart = true;
        boolean validEnd = true;
        while (validStart || validEnd) {
            log.debug("statics loop. startIndex : {}", startIndex);
            List<Map<String, String>> staticsList = getStaticsInfoHistoryFromRedis(clusterName, nodeId, fields, startIndex, endIndex);
            if (staticsList == null || staticsList.isEmpty()) {
                break;
            }
            for (Map<String, String> statics : staticsList) {
                long timestamp = Long.valueOf(statics.get("_timestamp"));
                if (timestamp > end) {
                    validEnd = false;
                } else if (timestamp < start) {
                    validStart = false;
                } else {
                    rangeResult.add(statics);
                }
            }
            startIndex += OFFSET + 1;
            endIndex += OFFSET + 1;
        }
        Collections.reverse(rangeResult);

        long thresholdMillis = getThresholdMillis(start, end);
        return getAveragedStaticsInfoHistory(rangeResult, fields, thresholdMillis);
    }

    @Override
    public List<SlowLog> getSlowLogAndReset(ClusterNode clusterNode) {
        try (Jedis jedis = jedisSupport.getJedisByHostAndPort(clusterNode.getHostAndPort())) {
            List<Slowlog> slowLogs = jedis.slowlogGet(100);
            jedis.slowlogReset();

            return slowLogs.stream()
                    .map(v -> SlowLog.builder()
                            .id(v.getId()).nodeId(clusterNode.getNodeId()).hostAndPort(clusterNode.getHostAndPort())
                            .timeStamp(v.getTimeStamp()).executionTime(v.getExecutionTime())
                            .args(v.getArgs()).build()).collect(Collectors.toList());
        }
    }

    long getThresholdMillis(long start, long end) {
        long durationMillis = end - start;
        if (durationMillis <= (long) 1 * 24 * 60 * 60 * 1000) { // 1day
            return Long.MIN_VALUE;
        } else if (durationMillis <= (long) 7 * 24 * 60 * 60 * 1000) { // 7days
            return 5 * 60 * 1000; // 5 min
        } else if (durationMillis <= (long) 30 * 24 * 60 * 60 * 1000) { // 30 days
            return 30 * 60 * 1000; // 30 min
        } else if (durationMillis <= (long) 60 * 24 * 60 * 60 * 1000) { // 60 days
            return 1 * 60 * 60 * 1000; // 1 hour
        } else if (durationMillis <= (long) 120 * 24 * 60 * 60 * 1000) { // 120 days
            return 2 * 60 * 60 * 1000; // 2 hours
        } else if (durationMillis <= (long) 180 * 24 * 60 * 60 * 1000) { // 180 days
            return 6 * 60 * 60 * 1000; // 6 hours
        } else if (durationMillis <= (long) 365 * 24 * 60 * 60 * 1000) { // 1 years
            return 12 * 60 * 60 * 1000; // 12 hours
        } else {
            return 24 * 60 * 60 * 1000; // 24 hours
        }
    }

    Map<String, List<List<Object>>> getAveragedStaticsInfoHistory(List<Map<String, String>> rangeResult, List<String> fields, long thresholdMillis) {

        Map<String, List<List<Object>>> averageResult = Maps.newConcurrentMap();
        fields.parallelStream().forEach(field -> {
            List<List<Object>> fieldAverageResult = Lists.newArrayList();

            // init data
            long preTimestamp = 0;
            BigDecimal sum = new BigDecimal(0);
            long sumTs = 0;
            int count = 0;

            for (Map<String, String> val : rangeResult) {
                long timestamp = Long.valueOf(val.get("_timestamp"));
                BigDecimal value;
                try {
                    value = new BigDecimal(val.get(field));
                } catch (Exception e) {
                    break;
                }

                if (preTimestamp > timestamp) {
                    // this is invalid timestamp.
                    // Iterate data should be sorted. Maybe, there is some noisy data.
                    continue;
                }

                if (preTimestamp == 0) { // is first. average start point.
                    preTimestamp = timestamp;
                    sum = value;
                    sumTs = timestamp;
                    count = 1;
                    continue;
                }

                long curDurationMillis = timestamp - preTimestamp;
                if (curDurationMillis < thresholdMillis) {
                    log.debug("Within threshold. value={}, curDurationSecs={}, thresholdSecs={}", value, curDurationMillis
                            / 1000, thresholdMillis);
                    sum = sum.add(value);
                    sumTs += timestamp;
                    count++;
                } else {
                    if (count > 0) {
                        BigDecimal averageValue = sum.divide(new BigDecimal(count), 4, BigDecimal.ROUND_HALF_UP);
                        long averageTs = sumTs / count;
                        log.debug("ESCAPE threshold. averageValue={}, averageTs={}, sum={}, sumTs={}, sumCount={}", averageValue, averageTs, sum, sumTs, count);
                        fieldAverageResult.add(Lists.newArrayList(averageTs, averageValue.doubleValue()));
                    }

                    preTimestamp = timestamp; // average start point.
                    sum = value;
                    sumTs = timestamp;
                    count = 1;
                }
            }

            if (count > 0) {
                BigDecimal averageValue = sum.divide(new BigDecimal(count), 4, BigDecimal.ROUND_HALF_UP);
                long averageTs = sumTs / count;
                log.debug("ESCAPE threshold. averageValue={}, averageTs={}, sum={}, sumTs={}, sumCount={}", averageValue, averageTs, sum, sumTs, count);
                fieldAverageResult.add(Lists.newArrayList(averageTs, averageValue.doubleValue()));
            }

            averageResult.put(field, fieldAverageResult);
        });

        return averageResult;
    }

    @Override
    public void shutdown(String hostAndPort) {
        try (Jedis jedis = jedisSupport.getJedisByHostAndPort(hostAndPort)) {
            try {
                jedis.ping();
            } catch (Exception e) {
                log.warn("Redis error.", e);
                throw new InvalidParameterException(String.format("Failed to connect to Redis Cluster(%s). Please confirm.", hostAndPort));
            }
            jedis.shutdown();
        }
    }

    List<Map<String, String>> getStaticsInfoHistoryFromRedis(String clusterName, String nodeId,
                                                             List<String> fields, long startIndex, long endIndex) {
        List<Map<String, String>> result = Lists.newArrayList();

        try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
            List<String> rawResult = dataStoreJedis.lrange(Constants.getNodeStaticsInfoRedisKey(redisPrefixKey, clusterName, nodeId), startIndex, endIndex);
            rawResult.forEach(v -> {
                try {
                    Map<String, String> map = mapper.readValue(v, new TypeReference<Map<String, Object>>() {
                    });
                    result.add(map);
                } catch (Exception e) {
                    log.warn("Failed to parse json.", e);
                }
            });
        }

        return filterGetStaticsInfoHistory(result, fields);
    }

    private List<Map<String, String>> filterGetStaticsInfoHistory(List<Map<String, String>> staticsInfos,
                                                                  List<String> fields) {
        List<String> copyedFields = new ArrayList<>(fields);
        copyedFields.add("_timestamp");

        return staticsInfos.stream().map(v -> {
            Map<String, String> item = Maps.newHashMap();
            copyedFields.forEach(field -> item.put(field, v.get(field)));
            return item;
        }).collect(Collectors.toList());
    }
}
