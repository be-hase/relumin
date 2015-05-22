package com.behase.relumin.config;

import lombok.Data;

@Data
public class Notice {
	private NoticeMailConfig mail = new NoticeMailConfig();
	private NoticeHttp http = new NoticeHttp();
}
