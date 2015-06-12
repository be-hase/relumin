package com.behase.relumin.service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.Notice;
import com.behase.relumin.model.NoticeItem.NoticeOperator;
import com.behase.relumin.model.NoticeItem.NoticeType;
import com.behase.relumin.model.NoticeItem.NoticeValueType;
import com.behase.relumin.model.NoticeJob;
import com.behase.relumin.model.NoticeJob.ResultValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {
	@Value("${relumin.host:}")
	private String reluminHost;

	@Value("${server.port:}")
	private String serverPort;

	@Value("${notice.mail.from:}")
	private String noticeMailFrom;

	@Autowired
	private MailSender mailSender;

	@Autowired
	private ObjectMapper mapper;

	private ExecutorService senderService = Executors.newFixedThreadPool(10);

	@Override
	public void notify(final Cluster cluster, final Notice notice, final List<NoticeJob> jobs) {
		senderService.execute(new Runnable() {
			@Override
			public void run() {
				String from = StringUtils.defaultString(notice.getMail().getFrom(), noticeMailFrom);
				if (mailSender != null && StringUtils.isNotBlank(from)) {
					try {
						notifyByMail(cluster, notice, jobs);
					} catch (Exception e) {
						log.error("Failed to send mail.", e);
					}
				}
				if (StringUtils.isNotBlank(notice.getHttp().getUrl())) {
					try {
						notifyByHttp(cluster, notice, jobs);
					} catch (Exception e) {
						log.error("Failed to send Http.", e);
					}
				}
			}

			private void notifyByMail(final Cluster cluster, final Notice notice, final List<NoticeJob> jobs) {
				String lineSeparator = System.lineSeparator();

				StringBuilder text = new StringBuilder();
				text.append(String.format("Cluster name : %s%s", cluster.getClusterName(), lineSeparator));
				if (StringUtils.isNotBlank(reluminHost)) {
					text.append(String.format("Check this page : http://%s:%s/#/cluster/%s%s", reluminHost, serverPort, cluster.getClusterName(), lineSeparator));
				}
				text.append(String.format("%s", lineSeparator));

				int i = 0;
				for (NoticeJob job : jobs) {
					i++;
					text.append(String.format("#%s.%s", i, lineSeparator));
					text.append(String.format("Metrics type : %s%s", job.getItem().getMetricsType(), lineSeparator));
					text.append(String.format("Metrics name : %s%s", job.getItem().getMetricsName(), lineSeparator));
					text.append(String.format("Notification condition : %s %s %s%s",
						job.getItem().getMetricsName(),
						NoticeOperator.getNoticeOperator(job.getItem().getOperator()).getLabel(),
						NoticeValueType.STRING.getValue().equals(job.getItem().getValueType()) ? "\""
							+ job.getItem().getValue() + "\"" : job.getItem().getValue(),
						lineSeparator));
					text.append(String.format("Result values : %s", lineSeparator));
					for (ResultValue resultValue : job.getResultValues()) {
						if (NoticeType.CLUSTER_INFO.getValue().equals(job.getItem().getMetricsType())) {
							text.append(String.format("    %s%s", resultValue.getValue(), lineSeparator));
						} else {
							text.append(String.format("    %s (on %s)%s", resultValue.getValue(), resultValue.getHostAndPort(), lineSeparator));
						}
					}

					text.append(String.format("%s", lineSeparator));
				}

				String from = StringUtils.isNotBlank(notice.getMail().getFrom()) ? notice.getMail().getFrom()
					: noticeMailFrom;
				SimpleMailMessage message = new SimpleMailMessage();
				message.setFrom(StringUtils.trim(from));
				message.setTo(Splitter.on(",").splitToList(notice.getMail().getTo()).stream().map(v -> {
					return StringUtils.trim(v);
				}).toArray(size -> new String[size]));
				message.setSubject(String.format("[Relumin] Notification of \"%s\". Please check.", cluster.getClusterName()));
				message.setText(text.toString());

				log.info("Send mail. from={}, to={}", message.getFrom(), message.getTo());
				mailSender.send(message);
			}

			private void notifyByHttp(final Cluster cluster, final Notice notice, final List<NoticeJob> jobs)
					throws Exception {
				String data = mapper.writeValueAsString(jobs);
				Request.Post(notice.getHttp().getUrl()).connectTimeout(3000).socketTimeout(3000).bodyForm(Form.form().add("clusterName", cluster.getClusterName()).add("data", data).build()).execute();
			}
		});
	}
}
