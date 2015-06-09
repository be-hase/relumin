package com.behase.relumin.config;

import lombok.Data;

@Data
public class NoticeConfig {
	private NoticeMailConfig mail = new NoticeMailConfig();
}
