package com.behase.relumin.service;

import com.behase.relumin.model.*;
import com.behase.relumin.webconfig.WebConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.OutputCapture;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class NotifyServiceImplTest {
    @InjectMocks
    @Spy
    private NotifyServiceImpl service = new NotifyServiceImpl();

    @Mock
    private MailSender mailSender;

    @Spy
    private ObjectMapper mapper = WebConfig.MAPPER;

    @Rule
    public WireMockRule mockServer = new WireMockRule(30000);

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public OutputCapture capture = new OutputCapture();

    // test data
    private Cluster cluster = Cluster.builder()
            .clusterName("clusterName")
            .status("ok")
            .info(Maps.newHashMap())
            .nodes(Lists.newArrayList())
            .slots(Lists.newArrayList())
            .build();
    private Notice notice = Notice.builder()
            .mail(NoticeMail.builder().to("to1@example.com, , to2@example.com").build())
            .http(NoticeHttp.builder().url("http://localhost:30000/callback").build())
            .build();
    private List<NoticeJob> jobs = Lists.newArrayList(
            NoticeJob.builder()
                    .item(NoticeItem.builder()
                            .metricsType(NoticeItem.NoticeType.CLUSTER_INFO.getValue())
                            .metricsName("cluster_state")
                            .operator(NoticeItem.NoticeOperator.NE.getValue())
                            .valueType(NoticeItem.NoticeValueType.STRING.getValue())
                            .value("ok")
                            .build())
                    .resultValues(Lists.newArrayList(
                            NoticeJob.ResultValue.builder().value("fail").build()
                    ))
                    .build(),
            NoticeJob.builder()
                    .item(NoticeItem.builder()
                            .metricsType(NoticeItem.NoticeType.NODE_INFO.getValue())
                            .metricsName("used_memory")
                            .operator(NoticeItem.NoticeOperator.LE.getValue())
                            .valueType(NoticeItem.NoticeValueType.NUMBER.getValue())
                            .value("1000")
                            .build())
                    .resultValues(Lists.newArrayList(
                            NoticeJob.ResultValue.builder().nodeId("nodeId1").hostAndPort("localhost:10000").value("100").build(),
                            NoticeJob.ResultValue.builder().nodeId("nodeId2").hostAndPort("localhost:10001").value("100").build()
                    ))
                    .build()
    );

    @Before
    public void init() {
        Whitebox.setInternalState(service, "reluminHost", "localhost");
        Whitebox.setInternalState(service, "serverPort", "8080");
        Whitebox.setInternalState(service, "noticeMailFrom", "from@example.com");
    }

    @Test
    public void notify_test() throws Exception {
        doNothing().when(service).notifyByMail(any(), any(), anyList());
        doNothing().when(service).notifyByHttp(any(), any(), anyList());

        service.notify(cluster, notice, jobs);
    }

    @Test
    public void notifyByMail_mailSender_is_null_then_nothing() throws Exception {
        Whitebox.setInternalState(service, "mailSender", null);

        service.notifyByMail(cluster, notice, jobs);
        assertThat(capture.toString(), containsString("Ignore mail notice. Because no mail setting."));
    }

    @Test
    public void notifyByMail_from_is_blank_then_nothing() throws Exception {
        Whitebox.setInternalState(service, "noticeMailFrom", "");

        service.notifyByMail(cluster, notice, jobs);
        assertThat(capture.toString(), containsString("Ignore mail notice. Because mo mail-from setting."));
    }

    @Test
    public void notifyByMail_to_length_is_zero_then_nothing() throws Exception {
        Notice notice = Notice.builder()
                .mail(NoticeMail.builder().to("").build())
                .build();

        service.notifyByMail(cluster, notice, jobs);
        assertThat(capture.toString(), containsString("Ignore mail notice. Because no mail-to setting."));
    }


    @Test
    public void notifyByMail() throws Exception {
        doReturn("text").when(service).getSummaryText(any(), anyList());

        service.notifyByMail(cluster, notice, jobs);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("from@example.com");
        message.setTo(new String[]{"to1@example.com", "to2@example.com"});
        message.setSubject("[Relumin] Notification of \"clusterName\". Please check.");
        message.setText("text");
        verify(mailSender).send(message);
    }

    @Test
    public void notifyByHttp_url_is_blank_then_nothing() throws Exception {
        Notice notice = Notice.builder()
                .http(NoticeHttp.builder().url("").build())
                .build();

        service.notifyByHttp(cluster, notice, jobs);
        assertThat(capture.toString(), containsString("Ignore http notice. Because no http-url setting."));
    }

    @Test
    public void notifyByHttp() throws Exception {
        mockServer.stubFor(post(urlEqualTo("/callback"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("OK")));

        doReturn("text").when(service).getSummaryText(any(), anyList());

        Map<String, Object> body = Maps.newLinkedHashMap();
        body.put("clusterName", cluster.getClusterName());
        body.put("data", jobs);
        body.put("summaryText", "text");

        service.notifyByHttp(cluster, notice, jobs);

        mockServer.verify(postRequestedFor(urlEqualTo("/callback"))
                .withRequestBody(matchingJsonPath("clusterName"))
                .withRequestBody(matchingJsonPath("data"))
                .withRequestBody(matchingJsonPath("summaryText"))
        );
    }

    @Test
    public void getSummaryText() {
        String expected = "" +
                "Cluster name : clusterName\n" +
                "Check this page : http://localhost:8080/#/cluster/clusterName\n" +
                "\n" +
                "#1.\n" +
                "Metrics type : cluster_info\n" +
                "Metrics name : cluster_state\n" +
                "Notification condition : cluster_state != \"ok\"\n" +
                "Result values : \n" +
                "    fail\n" +
                "\n" +
                "#2.\n" +
                "Metrics type : node_info\n" +
                "Metrics name : used_memory\n" +
                "Notification condition : used_memory <= 1000\n" +
                "Result values : \n" +
                "    100 (on localhost:10000)\n" +
                "    100 (on localhost:10001)\n" +
                "\n" +
                "";

        String result = service.getSummaryText(cluster, jobs);
        assertThat(result, is(expected));
    }

}
