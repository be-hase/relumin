package com.behase.relumin.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import scala.collection.mutable.StringBuilder;

import com.behase.relumin.model.ClusterNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	public static JedisCluster getJedisClusterByHostAndPort(String hostAndPort) {
		String[] hostAndPortArray = StringUtils.split(hostAndPort, ":");
		return new JedisCluster(
			Sets.newHashSet(new HostAndPort(hostAndPortArray[0], Integer.valueOf(hostAndPortArray[1]))),
			2000,
			new JedisPoolConfig());
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
			String value = StringUtils.trim(eachArray[1]);
			map.put(key, value);
		}

		return map;
	}

	public static Map<String, String> parseClusterInfoResult(String result) {
		Map<String, String> map = Maps.newLinkedHashMap();

		String[] line = StringUtils.split(result, "\n");
		for (String each : line) {
			String[] eachArray = StringUtils.split(each, ":");
			if (eachArray.length != 2) {
				continue;
			}

			String key = StringUtils.trim(eachArray[0]);
			String value = StringUtils.trim(eachArray[1]);
			map.put(key, value);
		}

		return map;
	}

	public static List<ClusterNode> parseClusterNodesResult(String result, String hostAndPort) {
		List<ClusterNode> clusterNodes = Lists.newArrayList();
		for (String resultLine : StringUtils.split(result, "\n")) {
			ClusterNode clusterNode = new ClusterNode();

			String[] resultLineArray = StringUtils.split(resultLine);
			clusterNode.setNodeId(resultLineArray[0]);

			String eachHostAndPort = resultLineArray[1];
			log.debug("eachHostAndPort={}", resultLine);
			if (StringUtils.isBlank(hostAndPort)) {
				clusterNode.setHostAndPort(eachHostAndPort);
			} else {
				if (StringUtils.startsWith(eachHostAndPort, ":")) {
					clusterNode.setHostAndPort(hostAndPort);
				} else {
					String[] eachHostAndPortArray = StringUtils.split(eachHostAndPort, ":");
					if ("127.0.0.1".equals(eachHostAndPortArray[0]) || "localhost".equals(eachHostAndPortArray[0])) {
						clusterNode.setHostAndPort(hostAndPort);
					} else {
						clusterNode.setHostAndPort(eachHostAndPort);
					}
				}
			}

			String eachFlag = resultLineArray[2];
			List<String> eachFlagList = Arrays.asList(StringUtils.split(eachFlag, ","));
			Set<String> eachFlagSet = Sets.newLinkedHashSet(eachFlagList);
			clusterNode.setFlags(eachFlagSet);

			clusterNode.setMasterNodeId("-".equals(resultLineArray[3]) ? "" : resultLineArray[3]);

			clusterNode.setPingSent(Long.valueOf(resultLineArray[4]));
			clusterNode.setPongReceived(Long.valueOf(resultLineArray[5]));

			clusterNode.setConfigEpoch(Long.valueOf(resultLineArray[6]));

			clusterNode.setConnect("connected".equals(resultLineArray[7]));

			List<String> slots = Lists.newArrayList();
			for (int i = 8; i < resultLineArray.length; i++) {
				if (clusterNode.hasFlag("myself") && StringUtils.startsWith(resultLineArray[i], "[")) {
					String trimed = StringUtils.substring(resultLineArray[i], 1, -1);
					if (StringUtils.indexOf(trimed, "->-") != StringUtils.INDEX_NOT_FOUND) {
						String[] trimedArray = StringUtils.split(trimed, "->-");
						clusterNode.getMigration().put(trimedArray[0], trimedArray[1]);
					} else if (StringUtils.indexOf(trimed, "-<-") != StringUtils.INDEX_NOT_FOUND) {
						String[] trimedArray = StringUtils.split(trimed, "-<-");
						clusterNode.getImporting().put(trimedArray[0], trimedArray[1]);
					}
				} else {
					slots.add(resultLineArray[i]);
				}
			}

			slots.forEach(v -> {
				if (StringUtils.indexOf(v, "-") == StringUtils.INDEX_NOT_FOUND) {
					clusterNode.getServedSlotsSet().add(Integer.valueOf(v));
				} else {
					String[] startAndEnd = StringUtils.split(v, "-");
					int start = Integer.valueOf(startAndEnd[0]);
					int end = Integer.valueOf(startAndEnd[1]);
					for (int i = start; i <= end; i++) {
						clusterNode.getServedSlotsSet().add(Integer.valueOf(i));
					}
				}
			});

			clusterNodes.add(clusterNode);
		}
		return clusterNodes;
	}

	public static String slotsDisplay(Collection<Integer> slots) {
		if (slots == null || slots.isEmpty()) {
			return "";
		}

		List<String> result = Lists.newArrayList();
		int i = 0;
		int first = 0;
		int last = 0;
		for (int current : slots) {
			// if first loop
			if (i == 0) {
				if (slots.size() == 1) {
					result.add(String.valueOf(current));
					break;
				}

				first = current;
				last = current;
				i++;
				continue;
			}

			if (current == last + 1) {
				// if last loop
				if (i == slots.size() - 1) {
					result.add(new StringBuilder().append(String.valueOf(first)).append("-").append(String.valueOf(current)).toString());
					break;
				}

				last = current;
				i++;
				continue;
			} else {
				// if last loop
				if (i == slots.size() - 1) {
					if (first == last) {
						result.add(String.valueOf(first));
					} else {
						result.add(new StringBuilder().append(String.valueOf(first)).append("-").append(String.valueOf(last)).toString());
					}
					result.add(String.valueOf(current));
					break;
				}

				if (first == last) {
					result.add(String.valueOf(first));
				} else {
					result.add(new StringBuilder().append(String.valueOf(first)).append("-").append(String.valueOf(last)).toString());
				}
				first = current;
				last = current;
				i++;
				continue;
			}
		}
		return StringUtils.join(result, ",");
	}
}
