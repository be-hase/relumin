package com.behase.relumin.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.behase.relumin.Constants;
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
	@Autowired
	JedisPool dataStoreJedisPool;

	@Autowired
	ObjectMapper mapper;

	@Override
	public Map<String, String> getStaticsInfo(ClusterNode clusterNode) {
		try (Jedis jedis = JedisUtils.getJedisByHostAndPort(clusterNode.getHostAndPort())) {
			Map<String, String> result = JedisUtils.parseInfoResult(jedis.info());

			// and add maxmemory for monitoring
			List<String> configResult = jedis.configGet("maxmemory");
			result.put("maxmemory", configResult.get(1));

			return result;
		}
	}

	@Override
	public List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end) {
		return getStaticsInfoHistory(clusterName, nodeId, start, end, true);
	}

	@Override
	public List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			boolean isTimeAsc) {
		List<Map<String, String>> result = Lists.newArrayList();
		try (Jedis jedis = dataStoreJedisPool.getResource()) {
			List<String> rawResult = jedis.lrange(Constants.getNodeStaticsInfoKey(clusterName, nodeId), start, end);
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

		if (!isTimeAsc) {
			Collections.reverse(result);
		}

		return result;
	}

	@Override
	public List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			List<String> fields) {
		List<Map<String, String>> staticsInfos = getStaticsInfoHistory(clusterName, nodeId, start, end);
		return filterGetStaticsInfoHistory(staticsInfos, fields);
	}

	@Override
	public List<Map<String, String>> getStaticsInfoHistory(String clusterName, String nodeId, long start, long end,
			List<String> fields, boolean isTimeAsc) {
		List<Map<String, String>> staticsInfos = getStaticsInfoHistory(clusterName, nodeId, start, end, isTimeAsc);
		return filterGetStaticsInfoHistory(staticsInfos, fields);
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
