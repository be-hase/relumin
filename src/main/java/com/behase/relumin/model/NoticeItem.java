package com.behase.relumin.model;

import java.util.Arrays;

import org.apache.commons.lang.StringUtils;

import lombok.Data;
import lombok.Getter;

@Data
public class NoticeItem {
	private String type;
	private String field;
	private String operator;
	private String valueType;
	private String value;

	public static enum NoticeType {
		CLUSTER_INFO("cluster_info"),
		NODE_STATICS("node_info");

		@Getter
		private String value;

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

		@Getter
		private String value;

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
		EQ("eq", " == "),
		NE("ne", " != "),
		GT("gt", " > "),
		GE("ge", " >= "),
		LT("lt", " < "),
		LE("le", " <= ");

		@Getter
		private String value;
		@Getter
		private String label;

		private NoticeOperator(String value, String label) {
			this.value = value;
			this.label = label;
		}

		public NoticeOperator getNoticeOperator(String value) {
			return Arrays.stream(values()).filter(v -> {
				return StringUtils.equalsIgnoreCase(v.value, value);
			}).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid operator"));
		}
	}
}
