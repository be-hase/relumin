package com.behase.relumin.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/cluster")
public class ClusterController {

	@RequestMapping(value = "/{name}/nodes", method = RequestMethod.GET)
	public Object nodes(
			@PathVariable String name
			) {
		log.debug(name);
		return name;
	}

	@RequestMapping(value = "/{name}/info", method = RequestMethod.GET)
	public Object info(
			@PathVariable String name
			) {
		log.debug(name);
		return name;
	}
}
