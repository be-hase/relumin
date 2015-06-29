package com.behase.relumin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class LoggingOperationServiceImpl implements LoggingOperationService {
	public static final String LOGGER_NAME = "logging_operation";
	private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);

	@Value("${auth.enabled}")
	private boolean authEnabled;

	@Override
	public void log(String operationName, Authentication authentication) {
		log(operationName, authentication, "-");
	}

	@Override
	public void log(String operationName, Authentication authentication, String msg) {
		String operator = getOperator(authentication);
		LOG.info("[{} by {}] {}", operationName, operator, msg);
	}

	@Override
	public void log(String operationName, Authentication authentication, String msg, Object... objects) {
		String operator = getOperator(authentication);
		String newMsg = String.format("[%s by %s] %s", operationName, operator, msg);
		LOG.info(newMsg, objects);
	}

	private String getOperator(Authentication authentication) {
		if (authentication == null) {
			return "Anonymous";
		}
		return authentication.getName();
	}
}
