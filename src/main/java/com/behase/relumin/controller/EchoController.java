package com.behase.relumin.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EchoController {

	@RequestMapping(value = "/echo", method = RequestMethod.POST)
	public Object echo(
			HttpServletRequest request
			) {
		return request.getParameterMap();
	}
}
