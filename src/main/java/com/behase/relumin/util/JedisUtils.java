package com.behase.relumin.util;

import org.apache.commons.lang3.StringUtils;

import redis.clients.jedis.Jedis;

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
}
