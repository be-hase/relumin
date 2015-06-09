package com.behase.relumin.model;

import java.util.List;

import com.google.common.collect.Lists;

import lombok.Data;

@Data
public class Notice {
	private NoticeMail mail = new NoticeMail();
	private NoticeHttp http = new NoticeHttp();
	private String invalidEndTime;
	private List<NoticeItem> items = Lists.newArrayList();
}
