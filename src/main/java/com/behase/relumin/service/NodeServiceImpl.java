package com.behase.relumin.service;

import com.behase.relumin.Constants;
import com.behase.relumin.exception.ApiException;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.support.JedisSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

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
            Map<String, String> result = jedisSupport.parseInfoResult(jedis.info());

            return result;
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
        while (true && (validStart || validEnd)) {
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

        long durationMillis = end - start;
        long thresholdMillis;
        if (durationMillis <= (long) 1 * 24 * 60 * 60 * 1000) { // 1day
            thresholdMillis = Long.MIN_VALUE;
        } else if (durationMillis <= (long) 7 * 24 * 60 * 60 * 1000) { // 7days
            thresholdMillis = 5 * 60 * 1000;
        } else if (durationMillis <= (long) 30 * 24 * 60 * 60 * 1000) { // 30 days
            thresholdMillis = 30 * 60 * 1000;
        } else if (durationMillis <= (long) 60 * 24 * 60 * 60 * 1000) { // 60 days
            thresholdMillis = 1 * 60 * 60 * 1000;
        } else if (durationMillis <= (long) 120 * 24 * 60 * 60 * 1000) { // 120 days
            thresholdMillis = 2 * 60 * 60 * 1000;
        } else if (durationMillis <= (long) 120 * 24 * 60 * 60 * 1000) { // 180 days
            thresholdMillis = 6 * 60 * 60 * 1000;
        } else if (durationMillis <= (long) 365 * 24 * 60 * 60 * 1000) { // 1 years
            thresholdMillis = 12 * 60 * 60 * 1000;
        } else {
            thresholdMillis = 24 * 60 * 60 * 1000;
        }

        Map<String, List<List<Object>>> averageResult = Maps.newConcurrentMap();
        fields.parallelStream().forEach(field -> {
            List<List<Object>> fieldAverageResult = Lists.newArrayList();
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
                    continue;
                }

                if (preTimestamp == 0) {
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
                    BigDecimal averageValue = new BigDecimal(0);
                    if (count != 0) {
                        averageValue = sum.divide(new BigDecimal(count), 4, BigDecimal.ROUND_HALF_UP);
                    }
                    long averageTs = 0;
                    if (count != 0) {
                        averageTs = sumTs / count;
                    }
                    log.debug("ESCAPE threshold. averageValue={}, averageTs={}, sum={}, sumTs={}, sumCount={}", averageValue, averageTs, sum, sumTs, count);
                    fieldAverageResult.add(Lists.newArrayList(averageTs, averageValue.doubleValue()));

                    preTimestamp = 0;
                    sum = new BigDecimal(0);
                    sumTs = 0;
                    count = 0;
                }
            }

            if (preTimestamp != 0) {
                BigDecimal averageValue = new BigDecimal(0);
                if (count != 0) {
                    averageValue = sum.divide(new BigDecimal(count), 4, BigDecimal.ROUND_HALF_UP);
                }
                long averageTs = 0;
                if (count != 0) {
                    averageTs = sumTs / count;
                }
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
                log.warn("redis error.", e);
                throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, String.format("Failed to connect to Redis Cluster(%s). Please confirm.", hostAndPort), HttpStatus.BAD_REQUEST);
            }
            jedis.shutdown();
        }
    }

    List<Map<String, String>> getStaticsInfoHistoryFromRedis(String clusterName, String nodeId,
                                                             List<String> fields, long startIndex, long endIndex) {
        List<Map<String, String>> result = Lists.newArrayList();

        try (Jedis jedis = dataStoreJedisPool.getResource()) {
            List<String> rawResult = jedis.lrange(Constants.getNodeStaticsInfoRedisKey(redisPrefixKey, clusterName, nodeId), startIndex, endIndex);
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

    List<Map<String, String>> filterGetStaticsInfoHistory(List<Map<String, String>> staticsInfos,
                                                          List<String> fields) {
        List<String> copyedFields = new ArrayList<String>(fields);
        copyedFields.add("_timestamp");

        return staticsInfos.stream().map(v -> {
            Map<String, String> item = Maps.newHashMap();
            copyedFields.forEach(field -> {
                item.put(field, v.get(field));
            });
            return item;
        }).collect(Collectors.toList());
    }
}
