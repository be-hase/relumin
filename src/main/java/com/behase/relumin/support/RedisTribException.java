package com.behase.relumin.support;

import lombok.Getter;

public class RedisTribException extends Exception {
	private static final long serialVersionUID = -2897762127023483847L;

	@Getter
	private boolean internalError = false;

	public RedisTribException(String message, boolean internalError) {
		super(message);
		this.internalError = internalError;
	}

	public RedisTribException(String message, boolean internalError, Throwable cause) {
		super(message, cause);
		this.internalError = internalError;
	}
}
