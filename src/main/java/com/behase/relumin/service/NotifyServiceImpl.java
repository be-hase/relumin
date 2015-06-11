package com.behase.relumin.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.stereotype.Service;

import com.behase.relumin.model.Notice;
import com.behase.relumin.model.NoticeJob;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {
	@Value("${notice.mail.from:}")
	private String noticeMailFrom;

	@Autowired
	private MailSender mailSender;

	@Autowired
	private ObjectMapper mapper;

	private ExecutorService senderService = Executors.newFixedThreadPool(10);

	@Override
	public void notify(final Notice notice, final List<NoticeJob> jobs) {
		senderService.execute(new Runnable() {
			@Override
			public void run() {
				String from = StringUtils.defaultString(notice.getMail().getFrom(), noticeMailFrom);
				if (mailSender != null && StringUtils.isNotBlank(from)) {
					try {
						notifyByMail(notice, jobs);
					} catch (Exception e) {
						log.error("Failed to send mail.", e);
					}
				}
				if (StringUtils.isNotBlank(notice.getHttp().getUrl())) {
					try {
						notifyByHttp(notice, jobs);
					} catch (Exception e) {
						log.error("Failed to send Http.", e);
					}
				}
			}

			private void notifyByMail(final Notice notice, final List<NoticeJob> jobs) {

			}

			private void notifyByHttp(final Notice notice, final List<NoticeJob> jobs) throws Exception {
				String body = mapper.writeValueAsString(jobs);
				log.debug("body = {}", body);
				Request.Post(notice.getHttp().getUrl()).connectTimeout(3000).socketTimeout(3000).bodyString(body, ContentType.APPLICATION_JSON).execute();
			}
		});
	}
}
