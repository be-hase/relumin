package com.behase.relumin.config;

import lombok.Data;

@Data
public class LogConfig {
	public static final String DEFAULT_TYPE = "file";
	public static final String DEFAULT_DIR = "logs";
	public static final String DEFAULT_LEVEL = "WARN";

	private String type = DEFAULT_TYPE;
	private String dir = DEFAULT_DIR;
	private String level = DEFAULT_LEVEL;
}
