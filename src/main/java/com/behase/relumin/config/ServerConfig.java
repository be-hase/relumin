package com.behase.relumin.config;

import lombok.Data;

@Data
public class ServerConfig {
	public static final String DEFAULT_PORT = "8080";
	public static final String DEFAULT_MONITOR_PORT = "20080";

	private String port;
	private String monitorPort;
}
