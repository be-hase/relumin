package com.behase.relumin.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NoticeJob {
	private NoticeItem item;
	private List<ResultValue> resultValues;

	@Data
	@AllArgsConstructor
	public static class ResultValue {
		private String nodeId;
		private String hostAndPort;
		private String value;
	}
}
