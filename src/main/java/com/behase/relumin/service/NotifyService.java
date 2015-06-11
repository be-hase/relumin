package com.behase.relumin.service;

import java.util.List;

import com.behase.relumin.model.Notice;
import com.behase.relumin.model.NoticeJob;

public interface NotifyService {
	void notify(Notice notice, List<NoticeJob> jobs);
}
