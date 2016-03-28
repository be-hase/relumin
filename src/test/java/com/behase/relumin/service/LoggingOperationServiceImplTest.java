package com.behase.relumin.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.boot.test.OutputCapture;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;

public class LoggingOperationServiceImplTest {
    private LoggingOperationServiceImpl service;

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Before
    public void init() {
        service = new LoggingOperationServiceImpl();
    }

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
