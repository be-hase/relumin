package com.behase.relumin.config;

import java.io.IOException;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import lombok.Data;

@Data
public class ReluminConfig {
	private ServerConfig server = new ServerConfig();
	private RedisConfig redis = new RedisConfig();
	private SchedulerConfig scheduler = new SchedulerConfig();

	public static ReluminConfig create(String configLocation) throws JsonParseException, JsonMappingException,
			IOException {
		ClassPathResource configResource = new ClassPathResource(configLocation);
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		return mapper.readValue(configResource.getInputStream(), ReluminConfig.class);
	}

	public Properties getProperties() {
		Properties prop = new Properties();
		prop.setProperty("server.port", server.getPort());
		prop.setProperty("management.port", server.getMonitorPort());
		prop.setProperty("redis.host", redis.getHost());
		prop.setProperty("redis.port", redis.getPort());
		return prop;
	}
}
