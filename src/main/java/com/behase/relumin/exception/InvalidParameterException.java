package com.behase.relumin.exception;

import com.behase.relumin.Constants;
import org.springframework.http.HttpStatus;

public class InvalidParameterException extends ApiException {
    private static final long serialVersionUID = -6416726302671334601L;

    public InvalidParameterException(String message) {
        super(Constants.ERR_CODE_INVALID_PARAMETER, String.format("Invalid parameter. %s", message), HttpStatus.BAD_REQUEST);
    }
}
