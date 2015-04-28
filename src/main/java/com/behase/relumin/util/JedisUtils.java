package com.behase.relumin.util;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import redis.clients.jedis.Jedis;

import com.behase.relumin.model.ClusterInfo;
import com.google.common.base.CaseFormat;
import com.google.common.collect.Maps;

public class JedisUtils {
	private JedisUtils() {

	}

	public static Jedis getJedisByHostAndPort(String hostAndPort, int timeout) {
		String[] hostAndPortArray = StringUtils.split(hostAndPort, ":");
		return new Jedis(hostAndPortArray[0], Integer.valueOf(hostAndPortArray[1]), timeout);
	}

	public static Jedis getJedisByHostAndPort(String hostAndPort) {
		String[] hostAndPortArray = StringUtils.split(hostAndPort, ":");
		return new Jedis(hostAndPortArray[0], Integer.valueOf(hostAndPortArray[1]));
	}

	public static Map<String, String> parseInfoResult(String result) {
		Map<String, String> map = Maps.newLinkedHashMap();
		map.put("_timestamp", String.valueOf(System.currentTimeMillis()));

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

	public static ClusterInfo parseClusterInfoResult(String result) {
		ClusterInfo clusterInfo = new ClusterInfo();

		String[] line = StringUtils.split(result, "\n");
		for (String each : line) {
			String[] eachArray = StringUtils.split(each, ":");
			if (eachArray.length != 2) {
				continue;
			}

			String key = StringUtils.trim(eachArray[0]);
			String value = StringUtils.trim(eachArray[1]);

			switch (key) {
				case "cluster_state":
					clusterInfo.setOk("ok".equals(value));
					break;
				case "cluster_slots_assigned":
					clusterInfo.setSlotsAssigned(Integer.valueOf(value));
					break;
				case "cluster_slots_ok":
					clusterInfo.setSlotsOk(Integer.valueOf(value));
					break;
				case "cluster_slots_pfail":
					clusterInfo.setSlotsPfail(Integer.valueOf(value));
					break;
				case "cluster_slots_fail":
					clusterInfo.setSlotsFail(Integer.valueOf(value));
					break;
				case "cluster_known_nodes":
					clusterInfo.setKnownNodes(Integer.valueOf(value));
					break;
				case "cluster_size":
					clusterInfo.setSize(Integer.valueOf(value));
					break;
				case "cluster_current_epoch":
					clusterInfo.setCurrentEpoch(Integer.valueOf(value));
					break;
				case "cluster_stats_messages_sent":
					clusterInfo.setStatsMessagesSent(Long.valueOf(value));
					break;
				case "cluster_stats_messages_received":
					clusterInfo.setStatsMessagesReceived(Long.valueOf(value));
					break;
				default:
					break;
			}
		}

		return clusterInfo;
	}
}
