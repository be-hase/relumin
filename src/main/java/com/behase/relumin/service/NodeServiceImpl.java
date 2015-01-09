package com.behase.relumin.service;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;

import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.util.JedisUtils;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NodeServiceImpl implements NodeService {
	@Override
	public Map<String, Object> getStaticsInfo(ClusterNode clusterNode) {
		try (Jedis jedis = JedisUtils.getJedisByHostAndPort(clusterNode.getHostAndPort())) {
			String result = jedis.info();
			return parseStaticdInfoResult(result);
		}
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
