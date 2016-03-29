package com.behase.relumin.service;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.springframework.boot.test.OutputCapture;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class LoggingOperationServiceImplTest {
    @InjectMocks
    @Spy
    private LoggingOperationServiceImpl service = new LoggingOperationServiceImpl();

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Test
    public void log_operationName_authentication() {
        service.log("operationName", null);
        assertThat(capture.toString(), containsString("[operationName by Anonymous] -"));
    }

    @Test
    public void log_operationName_authentication_msg() {
        service.log("operationName", null, "message");
        assertThat(capture.toString(), containsString("[operationName by Anonymous] message"));
    }

    @Test
    public void log_operationName_authentication_msg_objects() {
        service.log("operationName", null, "{} {}", "hoge", "bar");
        assertThat(capture.toString(), containsString("[operationName by Anonymous] hoge bar"));
    }
}
