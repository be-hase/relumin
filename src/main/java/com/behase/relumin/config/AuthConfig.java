package com.behase.relumin.config;

import lombok.Data;

@Data
public class AuthConfig {
	public static final String DEFAULT_ENABLED = "false";
	public static final String DEFAULT_LOGGING_OPERATION = "true";
	private String enabled;
	private String loggingOperation;
}
