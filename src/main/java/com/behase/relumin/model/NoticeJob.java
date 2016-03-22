package com.behase.relumin.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
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
