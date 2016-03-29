package com.behase.relumin.model;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ErrorResponseTest {
    @Test
    public void constructor() {
        ErrorResponse errorResponse = new ErrorResponse("code", "message");
        assertThat(errorResponse.getError().getCode(), is("code"));
        assertThat(errorResponse.getError().getMessage(), is("message"));
    }
}
