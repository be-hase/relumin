package com.behase.relumin.model;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;

import lombok.Data;

@Data
public class NoticeItem {
	private String type;
	private String filed;
	private String operator;
	private String valueType;
	private String value;

	public static enum NoticeType {
		CLUSTER_INFO("cluster_info"),
		NODE_STATICS("node_metrics");

		public String value;

		private NoticeType(String value) {
			this.value = value;
		}

		public NoticeType getNoticeType(String value) {
			return Arrays.stream(values()).filter(v -> {
				return StringUtils.equalsIgnoreCase(v.value, value);
			}).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid type"));
		}
	}

	public static enum NoticeValueType {
		NUMBER("number"),
		STRING("string");

		public String value;

		private NoticeValueType(String value) {
			this.value = value;
		}

		public NoticeValueType getNoticeValueType(String value) {
			return Arrays.stream(values()).filter(v -> {
				return StringUtils.equalsIgnoreCase(v.value, value);
			}).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid value type"));
		}
	}

	public static enum NoticeOperator {
		EQ("eq"),
		NE("ne"),
		GT("gt"),
		GE("ge"),
		LT("lt"),
		LE("le");

		public String value;

		private NoticeOperator(String value) {
			this.value = value;
		}

		public NoticeOperator getNoticeOperator(String value) {
			return Arrays.stream(values()).filter(v -> {
				return StringUtils.equalsIgnoreCase(v.value, value);
			}).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid operator"));
		}
	}
}
