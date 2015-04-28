package com.behase.relumin.support;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import redis.clients.jedis.Jedis;

import com.behase.relumin.model.ClusterInfo;
import com.behase.relumin.util.JedisUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TribClusterNode implements Closeable {
	private Jedis jedis;
	private Info info;
	private boolean dirty = false;
	private List<Info> friends = Lists.newArrayList();

	public TribClusterNode(String hostAndPort) {
		String[] hostAndPortArray = StringUtils.split(hostAndPort, ":");
		checkArgument(hostAndPortArray.length >= 2, "Invalid IP or Port. Use IP:Port format");

		info = new Info();
		info.setHost(hostAndPortArray[0]);
		info.setPort(Integer.valueOf(hostAndPortArray[1]));
	}

	public List<Info> getFriends() {
		return friends;
	}

	public Map<String, String> getSlots() {
		return info.getSlots();
	}

	public boolean hasFlag(String flag) {
		return info.getFlags().contains(flag);
	}

	public void connect() {
		connect(false);
	}

	public void connect(boolean abort) {
		if (jedis != null) {
			return;
		}

		try {
			jedis = new Jedis(info.getHost(), info.getPort());
			if (!"PONG".equalsIgnoreCase(jedis.ping())) {
				log.error("Invalid PONG-message.");
				throw new IllegalArgumentException("Invalid PONG-message.");
			}
		} catch (Exception e) {
			log.error("Failed to connect to node.");
			if (abort) {
				throw new IllegalArgumentException("Failed to connect to node.");
			}
			jedis = null;
		}
	}

	public void assertCluster() {
		Map<String, String> infoResult = JedisUtils.parseInfoResult(jedis.info());
		String clusterEnabled = infoResult.get("clusterEnabled");
		if (StringUtils.isBlank(clusterEnabled) || StringUtils.equals(clusterEnabled, "0")) {
			throw new IllegalArgumentException(String.format("%s:%s is not configured as a cluster node.", info.getHost(), info.getPort()));
		}
	}

	public void assertEmpty() {
		Map<String, String> infoResult = JedisUtils.parseInfoResult(jedis.info());
		ClusterInfo clusterInfo = JedisUtils.parseClusterInfoResult(jedis.clusterInfo());
		if (infoResult.get("db0") != null || clusterInfo.getKnownNodes() != 1) {
			throw new IllegalArgumentException(String.format("%s:%s is not empty. Either the node already knows other nodes (check with CLUSTER NODES) or contains some key in database 0.", info.getHost(), info.getPort()));
		}
	}

	public void loadInfo() {
		loadInfo(false);
	}

	public void loadInfo(boolean getFriend) {
		connect();
		//now developing
	}

	public Info getInfo() {
		return info;
	}

	public boolean isDirty() {
		return dirty;
	}

	public Jedis getJedis() {
		return jedis;
	}

	@Override
	public void close() throws IOException {
		if (jedis != null) {
			jedis.close();
		}
	}

	@Setter
	@Getter
	public static class Info {
		private String host;
		private int port;
		private Set<String> flags = Sets.newHashSet();
		private Map<String, String> slots = Maps.newHashMap();
		private Map<String, String> migrating = Maps.newHashMap();
		private Map<String, String> importing = Maps.newHashMap();
		private boolean replicate = false;
	}

}
