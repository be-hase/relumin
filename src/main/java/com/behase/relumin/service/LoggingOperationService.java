package com.behase.relumin.service;

import org.springframework.security.core.Authentication;

public interface LoggingOperationService {
	void log(String operationName, Authentication authentication);

	void log(String operationName, Authentication authentication, String msg);

	void log(String operationName, Authentication authentication, String msg, Object... objects);
}
