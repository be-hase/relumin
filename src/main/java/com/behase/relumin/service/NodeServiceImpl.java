package com.behase.relumin.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.behase.relumin.Constants;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.util.JedisUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NodeServiceImpl implements NodeService {
	@Autowired
	JedisPool dataStoreJedisPool;

	@Autowired
	ObjectMapper mapper;

	@Override
	public Map<String, Object> getStaticsInfo(ClusterNode clusterNode) {
		try (Jedis jedis = JedisUtils.getJedisByHostAndPort(clusterNode.getHostAndPort())) {
			Map<String, Object> result = parseStaticdInfoResult(jedis.info());

			// and add maxmemory for monitoring
			List<String> configResult = jedis.configGet("maxmemory");
			result.put("maxmemory", configResult.get(1));

			return result;
		}
	}

	@Override
	public List<Map<String, Object>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end) {
		return getStaticsInfoHistory(clusterName, nodeId, start, end, true);
	}

	@Override
	public List<Map<String, Object>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			boolean isTimeAsc) {
		List<Map<String, Object>> result = Lists.newArrayList();
		try (Jedis jedis = dataStoreJedisPool.getResource()) {
			List<String> rawResult = jedis.lrange(Constants.getNodeStaticsInfoKey(clusterName, nodeId), start, end);
			rawResult.forEach(v -> {
				try {
					Map<String, Object> map = mapper.readValue(v, new TypeReference<Map<String, Object>>() {
					});
					result.add(map);
				} catch (Exception e) {
					log.warn("Failed to parse json.", e);
				}
			});
		}

		if (!isTimeAsc) {
			Collections.reverse(result);
		}

		return result;
	}

	@Override
	public List<Map<String, Object>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			List<String> fields) {
		List<Map<String, Object>> staticsInfos = getStaticsInfoHistory(clusterName, nodeId, start, end);
		return filterGetStaticsInfoHistory(staticsInfos, fields);
	}

	@Override
	public List<Map<String, Object>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			List<String> fields, boolean isTimeAsc) {
		List<Map<String, Object>> staticsInfos = getStaticsInfoHistory(clusterName, nodeId, start, end, isTimeAsc);
		return filterGetStaticsInfoHistory(staticsInfos, fields);
	}

	private List<Map<String, Object>> filterGetStaticsInfoHistory(List<Map<String, Object>> staticsInfos,
			List<String> fields) {
		if (fields.isEmpty()) {
			return staticsInfos;
		}
		fields.add("_timestamp");

		return staticsInfos.stream().map(v -> {
			Map<String, Object> item = Maps.newHashMap();
			fields.forEach(field -> {
				item.put(field, v.get(field));
			});
			return item;
		}).collect(Collectors.toList());
	}

	private Map<String, Object> parseStaticdInfoResult(String result) {
		Map<String, Object> map = Maps.newLinkedHashMap();
		map.put("_timestamp", System.currentTimeMillis());

		String[] line = StringUtils.split(result, "\n");
		for (String each : line) {
			String[] eachArray = StringUtils.split(each, ":");
			if (eachArray.length != 2) {
				continue;
			}
			String key = StringUtils.trim(eachArray[0]);
			key = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key);
			String value = StringUtils.trim(eachArray[1]);
			map.put(key, value);
		}

		return map;
	}
}
