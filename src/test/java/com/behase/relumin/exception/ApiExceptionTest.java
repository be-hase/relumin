package com.behase.relumin.exception;

import com.behase.relumin.model.ErrorResponse;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class ApiExceptionTest {
	@Test
	public void constructor() {
		ApiException apiException;

		apiException = new ApiException(new ErrorResponse("400", "message"), HttpStatus.BAD_REQUEST);
		assertThat(apiException.getErrorResponse(), is(new ErrorResponse("400", "message")));
		assertThat(apiException.getHttpStatus(), is(HttpStatus.BAD_REQUEST));

		apiException = new ApiException("400", "message", HttpStatus.BAD_REQUEST);
		assertThat(apiException.getErrorResponse(), is(new ErrorResponse("400", "message")));
		assertThat(apiException.getHttpStatus(), is(HttpStatus.BAD_REQUEST));
	}
}
