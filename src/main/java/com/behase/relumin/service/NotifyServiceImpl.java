package com.behase.relumin.service;

import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.Notice;
import com.behase.relumin.model.NoticeItem.NoticeOperator;
import com.behase.relumin.model.NoticeItem.NoticeType;
import com.behase.relumin.model.NoticeItem.NoticeValueType;
import com.behase.relumin.model.NoticeJob;
import com.behase.relumin.model.NoticeJob.ResultValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {
    @Value("${relumin.host}")
    private String reluminHost;

    @Value("${server.port}")
    private String serverPort;

    @Value("${notice.mail.from}")
    private String noticeMailFrom;

    @Autowired
    private MailSender mailSender;

    @Autowired
    private ObjectMapper mapper;

    private ExecutorService senderService = Executors.newFixedThreadPool(10);

    @PreDestroy
    public void preDestroy() {
        if (senderService != null) {
            senderService.shutdown();
            try {
                if (senderService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                    senderService.shutdownNow();
                }
            } catch (InterruptedException e) {
                senderService.shutdownNow();
            }
        }
    }

    @Override
    public void notify(final Cluster cluster, final Notice notice, final List<NoticeJob> jobs) {
        senderService.execute(() -> {
            try {
                notifyByMail(cluster, notice, jobs);
            } catch (Exception e) {
                log.error("Failed to send mail.", e);
            }
            try {
                notifyByHttp(cluster, notice, jobs);
            } catch (Exception e) {
                log.error("Failed to send Http.", e);
            }
        });
    }

    void notifyByMail(final Cluster cluster, final Notice notice, final List<NoticeJob> jobs) {
        if (mailSender == null) {
            log.info("Ignore mail notice. Because no mail setting. clusterName={}", cluster.getClusterName());
            return;
        }

        String from = StringUtils.trim(StringUtils.defaultIfBlank(notice.getMail().getFrom(), noticeMailFrom));
        List<String> toList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(notice.getMail().getTo());
        String[] to = toList.toArray(new String[toList.size()]);

        if (StringUtils.isBlank(from)) {
            log.info("Ignore mail notice. Because mo mail-from setting. clusterName={}", cluster.getClusterName());
            return;
        }
        if (to.length == 0) {
            log.info("Ignore mail notice. Because no mail-to setting. clusterName={}", cluster.getClusterName());
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(String.format("[Relumin] Notification of \"%s\". Please check.", cluster.getClusterName()));
        message.setText(getSummaryText(cluster, jobs));

        log.info("Send mail. from={}, to={}", message.getFrom(), message.getTo());
        mailSender.send(message);
    }

    void notifyByHttp(final Cluster cluster, final Notice notice, final List<NoticeJob> jobs)
            throws Exception {
        String httpUrl = notice.getHttp().getUrl();

        if (StringUtils.isBlank(httpUrl)) {
            log.info("Ignore http notice. Because no http-url setting. clusterName={}", cluster.getClusterName());
            return;
        }

        Map<String, Object> body = Maps.newLinkedHashMap();
        body.put("clusterName", cluster.getClusterName());
        body.put("data", jobs);
        body.put("summaryText", jobs);

        Request.Post(httpUrl)
                .connectTimeout(3000)
                .socketTimeout(3000)
                .bodyString(mapper.writeValueAsString(body), ContentType.APPLICATION_JSON)
                .execute();
    }

    String getSummaryText(final Cluster cluster, final List<NoticeJob> jobs) {
        StringBuilder text = new StringBuilder();
        String lineSeparator = System.lineSeparator();

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
            text.append(String.format("Notification condition : %s%s%s%s",
                    job.getItem().getMetricsName(),
                    NoticeOperator.getNoticeOperator(job.getItem().getOperator()).getLabel(),
                    NoticeValueType.STRING.getValue().equals(job.getItem().getValueType()) ? "\"" + job.getItem().getValue() + "\"" : job.getItem().getValue(),
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

        return text.toString();
    }
}
