package org.relumin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

public class LoggingOperationService {
    public static final String LOGGER_NAME = "logging_operation";
    private static final Logger LOG = LoggerFactory.getLogger(LOGGER_NAME);

    public void log(String operationName, Authentication authentication) {
        log(operationName, authentication, "-");
    }

    public void log(String operationName, Authentication authentication, String msg) {
        String operator = getOperator(authentication);
        LOG.info("[{} by {}] {}", operationName, operator, msg);
    }

    public void log(String operationName, Authentication authentication, String msg, Object... objects) {
        String operator = getOperator(authentication);
        String newMsg = String.format("[%s by %s] %s", operationName, operator, msg);
        LOG.info(newMsg, objects);
    }

    String getOperator(Authentication authentication) {
        if (authentication == null) {
            return "Anonymous";
        }
        return authentication.getName();
    }
}
