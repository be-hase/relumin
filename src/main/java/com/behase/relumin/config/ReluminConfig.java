package com.behase.relumin.config;

import java.util.Properties;

import lombok.Data;

@Data
public class ReluminConfig {
	private ServerConfig server = new ServerConfig();
	private LogConfig log = new LogConfig();
	private RedisConfig redis = new RedisConfig();

	public Properties getProperties() {
		Properties prop = new Properties();
		prop.setProperty("server.port", server.getPort());
		prop.setProperty("management.port", server.getMonitorPort());
		prop.setProperty("redis.host", redis.getHost());
		prop.setProperty("redis.port", redis.getPort());
		return prop;
	}
}
