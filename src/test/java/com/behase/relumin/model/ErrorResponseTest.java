package com.behase.relumin.model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;

public class ErrorResponseTest {
    @Test
    public void constructor() {
        ErrorResponse errorResponse = new ErrorResponse("code", "message");
        assertThat(errorResponse.getError().getCode(), is("code"));
        assertThat(errorResponse.getError().getMessage(), is("message"));
    }
}