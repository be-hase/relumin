package com.behase.relumin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class WebController {
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public Object getClusterList(
			) {
		return "/index";
	}
}
