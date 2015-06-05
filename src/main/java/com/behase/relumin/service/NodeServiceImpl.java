package com.behase.relumin.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.behase.relumin.Constants;
import com.behase.relumin.exception.ApiException;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.util.JedisUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NodeServiceImpl implements NodeService {
	private static final int OFFSET = 9999;

	@Autowired
	JedisPool dataStoreJedisPool;

	@Autowired
	ObjectMapper mapper;

	@Value("${redis.prefixKey}")
	private String redisPrefixKey;

	@Override
	public Map<String, String> getStaticsInfo(ClusterNode clusterNode) {
		try (Jedis jedis = JedisUtils.getJedisByHostAndPort(clusterNode.getHostAndPort())) {
			Map<String, String> result = JedisUtils.parseInfoResult(jedis.info());

			return result;
		}
	}

	@Override
	public List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, List<String> fields,
			long start, long end) {
		List<Map<String, String>> result = Lists.newArrayList();

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
				Long timestamp = Long.valueOf(statics.get("_timestamp"));
				if (timestamp > end) {
					validEnd = false;
				} else if (timestamp < start) {
					validStart = false;
				} else {
					result.add(statics);
				}
			}
			startIndex += OFFSET + 1;
			endIndex += OFFSET + 1;
		}

		return result;
	}

	@Override
	public List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, List<String> fields,
			long start, long end, boolean isTimeAsc) {
		List<Map<String, String>> result = getStaticsInfoHistory(clusterName, nodeId, fields, start, end);
		if (!isTimeAsc) {
			Collections.reverse(result);
		}
		return result;
	}

	@Override
	public void shutdown(String hostAndPort) {
		try (Jedis jedis = JedisUtils.getJedisByHostAndPort(hostAndPort)) {
			try {
				jedis.ping();
			} catch (Exception e) {
				log.warn("redis error.", e);
				throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, String.format("Failed to connect to Redis Cluster(%s). Please confirm.", hostAndPort), HttpStatus.BAD_REQUEST);
			}
			jedis.shutdown();
		}
	}

	private List<Map<String, String>> getStaticsInfoHistoryFromRedis(String clusterName, String nodeId,
			List<String> fields, long startIndex, long endIndex) {
		List<Map<String, String>> result = Lists.newArrayList();

		try (Jedis jedis = dataStoreJedisPool.getResource()) {
			List<String> rawResult = jedis.lrange(Constants.getNodeStaticsInfoKey(redisPrefixKey, clusterName, nodeId), startIndex, endIndex);
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
		if (fields.isEmpty()) {
			return staticsInfos;
		}
		fields.add("_timestamp");

		return staticsInfos.stream().map(v -> {
			Map<String, String> item = Maps.newHashMap();
			fields.forEach(field -> {
				item.put(field, v.get(field));
			});
			return item;
		}).collect(Collectors.toList());
	}
}
