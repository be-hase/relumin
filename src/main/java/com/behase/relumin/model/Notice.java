package com.behase.relumin.model;

import java.util.List;

import com.google.common.collect.Lists;

import lombok.Data;

@Data
public class Notice {
	private NoticeMailConfig mail;
	private NoticeHttp http;
	private List<NoticeItem> items = Lists.newArrayList();
}
