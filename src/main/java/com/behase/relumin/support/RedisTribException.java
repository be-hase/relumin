package com.behase.relumin.support;

public class RedisTribException extends Exception {
	private static final long serialVersionUID = -2897762127023483847L;

	public RedisTribException(String message) {
		super(message);
	}

	public RedisTribException(String message, Throwable cause) {
		super(message, cause);
	}
}
