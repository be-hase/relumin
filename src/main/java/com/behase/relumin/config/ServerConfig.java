package com.behase.relumin.config;

import lombok.Data;

@Data
public class ServerConfig {
	public static final String DEFAULT_PORT = "10080";
	public static final String DEFAULT_MONITOR_PORT = "10081";

	private String port = DEFAULT_PORT;
	private String monitorPort = DEFAULT_MONITOR_PORT;
}
