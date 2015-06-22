package com.behase.relumin.config;

import lombok.Data;

@Data
public class OutputMetricsConfig {
	//	private OutputMetricsFileConfig file = new OutputMetricsFileConfig();
	private OutputMetricsFluentdConfig fluentd = new OutputMetricsFluentdConfig();

	//	@Data
	//	public static class OutputMetricsFileConfig {
	//		public static final String DEFAULT_ENABLED = "false";
	//		public static final String DEFAULT_NAME = "relumin_metrics";
	//		public static final String DEFAULT_DIR = ".";
	//		public static final String DEFAULT_MAX_SIZE = "500MB";
	//		public static final String DEFAULT_COUNT = "10";
	//
	//		private String enabled;
	//		private String name;
	//		private String dir;
	//		private String maxSize;
	//		private String count;
	//	}

	@Data
	public static class OutputMetricsFluentdConfig {
		public static final String DEFAULT_ENABLED = "false";
		public static final String DEFAULT_TIMEOUT = "3000";
		public static final String DEFAULT_BUFFER_CAPACITY = "1048576";
		public static final String DEFAULT_TAG = "relumin";
		public static final String DEFAULT_NODE_TAG = "node";

		private String enabled;
		private String host;
		private String port;
		private String timeout;
		private String bufferCapacity;
		private String tag;
		private String nodeTag;
	}
}
