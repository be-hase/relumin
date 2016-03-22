package com.behase.relumin.controller;

import com.behase.relumin.model.ErrorResponse;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

public class MyErrorControllerTest {
	private MyErrorController controller;

	@Before
	public void init() {
		controller = new MyErrorController();
	}

	@Test
	public void handleError_status_code_is_404_or_url_is_error_path_then_return_404_not_found() throws Exception {
		HttpServletRequest request;
		ResponseEntity<ErrorResponse> result;

		request = mock(HttpServletRequest.class);
		doReturn(404).when(request).getAttribute("javax.servlet.error.status_code");

		result = controller.handleError(request);
		assertThat(result.getBody().getError().getCode(), is("404"));
		assertThat(result.getBody().getError().getMessage(), is("Not Found."));

		request = mock(HttpServletRequest.class);
		doReturn("/error").when(request).getRequestURI();

		result = controller.handleError(request);
		assertThat(result.getBody().getError().getCode(), is("404"));
		assertThat(result.getBody().getError().getMessage(), is("Not Found."));
	}

	@Test
	public void handleError() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		doReturn(500).when(request).getAttribute("javax.servlet.error.status_code");
		doReturn("error").when(request).getAttribute("javax.servlet.error.message");

		ResponseEntity<ErrorResponse> result = controller.handleError(request);
		assertThat(result.getBody().getError().getCode(), is("500"));
		assertThat(result.getBody().getError().getMessage(), is("error"));
	}

	@Test
	public void getStatus() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		doReturn(200).when(request).getAttribute("javax.servlet.error.status_code");

		HttpStatus result = controller.getStatus(request);
		assertThat(result, is(HttpStatus.OK));
	}

	@Test
	public void getStatus_no_status() {
		HttpServletRequest request = mock(HttpServletRequest.class);

		doReturn(null).when(request).getAttribute("javax.servlet.error.status_code");

		HttpStatus result = controller.getStatus(request);
		assertThat(result, is(HttpStatus.INTERNAL_SERVER_ERROR));
	}

	@Test
	public void getMessage() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		doReturn("error").when(request).getAttribute("javax.servlet.error.message");

		String result = controller.getMessage(request);
		assertThat(result, is("error"));
	}

	@Test
	public void getMessage_no_message() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		doReturn(null).when(request).getAttribute("javax.servlet.error.message");

		String result = controller.getMessage(request);
		assertThat(result, is("No message."));
	}
}
