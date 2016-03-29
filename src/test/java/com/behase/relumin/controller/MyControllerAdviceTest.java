package com.behase.relumin.controller;

import com.behase.relumin.exception.ApiException;
import com.behase.relumin.model.ErrorResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.OutputCapture;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class MyControllerAdviceTest {
    @InjectMocks
    @Spy
    private MyControllerAdvice advice = new MyControllerAdvice();

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void allExceptionHandler() {
        ResponseEntity<ErrorResponse> result = advice.allExceptionHandler(
                new Exception(), null, null
        );
        assertThat(result.getBody().getError().getCode(), is("500"));
        assertThat(result.getBody().getError().getMessage(), is("Internal server error."));
    }

    @Test
    public void missingServletRequestParameterExceptionHandler() {
        ErrorResponse result = advice.missingServletRequestParameterExceptionHandler(
                new MissingServletRequestParameterException("", ""), null, null
        );
        assertThat(result.getError().getCode(), is("400"));
        assertThat(result.getError().getMessage(), is("Invalid parameter."));
    }

    @Test
    public void apiExceptionHandler_5xx() {
        ResponseEntity<ErrorResponse> result = advice.apiExceptionHandler(
                new ApiException("500", "error", HttpStatus.INTERNAL_SERVER_ERROR), null, null
        );
        assertThat(result.getBody().getError().getCode(), is("500"));
        assertThat(result.getBody().getError().getMessage(), is("error"));
        assertThat(capture.toString(), containsString("ERROR"));
    }

    @Test
    public void apiExceptionHandler_4xx() {
        ResponseEntity<ErrorResponse> result = advice.apiExceptionHandler(
                new ApiException("400", "error", HttpStatus.BAD_REQUEST), null, null
        );
        assertThat(result.getBody().getError().getCode(), is("400"));
        assertThat(result.getBody().getError().getMessage(), is("error"));
        assertThat(capture.toString(), containsString("WARN"));
    }

    @Test
    public void httpRequestMethodNotSupportedExceptionHandle() {
        ErrorResponse result = advice.httpRequestMethodNotSupportedExceptionHandle(
                new HttpRequestMethodNotSupportedException("GET", "error"), null
        );
        assertThat(result.getError().getCode(), is("405"));
        assertThat(result.getError().getMessage(), is("error"));
    }

    @Test
    public void httpMessageNotReadableExceptionHandle() {
        ErrorResponse result = advice.httpMessageNotReadableExceptionHandle(
                new HttpMessageNotReadableException("error"), null
        );
        assertThat(result.getError().getCode(), is("400"));
        assertThat(result.getError().getMessage(), is("Invalid parameter."));
    }
}
