package com.behase.relumin.service;

import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.Notice;
import com.behase.relumin.model.NoticeJob;

import java.util.List;

public interface NotifyService {
    void notify(Cluster cluster, Notice notice, List<NoticeJob> jobs);
}
