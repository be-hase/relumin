package com.behase.relumin.config;

import lombok.Data;

@Data
public class RedisConfig {
	public static final String DEFAULT_PREFIX_KEY = "_relumin";

	private String prefixKey;
	private String host;
	private String port;
}
