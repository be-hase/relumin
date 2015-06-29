package com.behase.relumin.service;

import org.springframework.security.core.Authentication;

public interface LoggingOperationService {
	public void log(String operationName, Authentication authentication);

	public void log(String operationName, Authentication authentication, String msg);

	public void log(String operationName, Authentication authentication, String msg, Object... objects);
}
