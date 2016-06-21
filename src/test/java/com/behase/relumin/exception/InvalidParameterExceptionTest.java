package com.behase.relumin.exception;

import com.behase.relumin.model.ErrorResponse;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class InvalidParameterExceptionTest {
    @Test
    public void constructor() {
        InvalidParameterException invalidParameterException;

        invalidParameterException = new InvalidParameterException("message");
        assertThat(invalidParameterException.getErrorResponse(), is(new ErrorResponse("400_000", "Invalid parameter. message")));
        assertThat(invalidParameterException.getHttpStatus(), is(HttpStatus.BAD_REQUEST));
    }
}
