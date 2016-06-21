package com.behase.relumin.exception;

import com.behase.relumin.model.ErrorResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private static final long serialVersionUID = 3285456520279227122L;

    @Getter
    @Setter
    private ErrorResponse errorResponse;

    @Getter
    @Setter
    private HttpStatus httpStatus;

    public ApiException(ErrorResponse errorResponse, HttpStatus httpStatus) {
        super(errorResponse.getError().getMessage());
        this.errorResponse = errorResponse;
        this.httpStatus = httpStatus;
    }

    public ApiException(String code, String message, HttpStatus httpStatus) {
        super(message);
        this.errorResponse = new ErrorResponse(code, message);
        this.httpStatus = httpStatus;
    }
}
