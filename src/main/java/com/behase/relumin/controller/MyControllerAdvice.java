package com.behase.relumin.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;

import com.behase.relumin.exception.ApiException;
import com.behase.relumin.model.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
@Configuration
public class MyControllerAdvice {
	@ExceptionHandler(Exception.class)
	@ResponseBody
	public ResponseEntity<ErrorResponse> allExceptionHandler(Exception e, HttpServletRequest request,
			HttpServletResponse response) {
		log.error("handle Exception.", e); // 意図していないExceptionなので、error logを出す。
		return new ResponseEntity<ErrorResponse>(new ErrorResponse("500", "Internal server error."), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ErrorResponse missingServletRequestParameterExceptionHandler(
			MissingServletRequestParameterException e,
			HttpServletRequest request,
			HttpServletResponse response) {
		log.warn("handle Exception.", e); // 意図しているExceptionなので、warn logを出す。
		return new ErrorResponse("400", "Invalid parameter.");
	}

	@ExceptionHandler(ApiException.class)
	@ResponseBody
	public ResponseEntity<Object> apiExceptionHandler(ApiException e, HttpServletRequest request,
			HttpServletResponse response) {
		if (e.getHttpStatus().is5xxServerError()) {
			log.error("handle Exception.", e);
		} else {
			log.warn("handle Exception.", e);
		}
		return new ResponseEntity<Object>(e.getErrorResponse(), e.getHttpStatus());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	@ResponseBody
	public ErrorResponse httpRequestMethodNotSupportedExceptionHandle(
			final HttpRequestMethodNotSupportedException e,
			final NativeWebRequest request) {
		log.warn("handle Exception.", e); // 意図しているExceptionなので、warn logを出す。
		return new ErrorResponse("405", e.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ResponseBody
	public ErrorResponse httpMessageNotReadableExceptionHandle(final HttpMessageNotReadableException e,
			final NativeWebRequest request) {
		log.warn("handle Exception.", e); // 意図しているExceptionなので、warn logを出す。
		return new ErrorResponse("400", "Invalid parameter.");
	}
}
