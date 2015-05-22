package com.behase.relumin.config;

import lombok.Data;

@Data
public class NoticeMailConfig {
	public static final String DEFAULT_CHARSET = "UTF-8";

	private String host;
	private String port;
	private String user;
	private String password;
	private String charset;
}
