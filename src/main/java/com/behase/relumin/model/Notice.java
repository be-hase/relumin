package com.behase.relumin.model;

import java.util.List;

import com.google.common.collect.Lists;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notice {
	private NoticeMail mail = new NoticeMail();
	private NoticeHttp http = new NoticeHttp();
	private String invalidEndTime;
	private List<NoticeItem> items = Lists.newArrayList();
}
