package com.behase.relumin.service;

import java.util.List;

import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.Notice;
import com.behase.relumin.model.NoticeJob;

public interface NotifyService {
	void notify(Cluster cluster, Notice notice, List<NoticeJob> jobs);
}
