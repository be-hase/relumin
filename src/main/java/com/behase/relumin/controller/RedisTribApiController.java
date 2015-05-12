package com.behase.relumin.controller;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.service.RedisTribService;
import com.behase.relumin.util.ValidationUtils;
import com.google.common.collect.Lists;

@RestController
@RequestMapping(value = "/api/trib")
public class RedisTribApiController {
	@Autowired
	private RedisTribService service;

	@RequestMapping(value = "/create-parameter", method = RequestMethod.GET)
	public List<CreateClusterParam> getCreateParameter(
			@RequestParam(defaultValue = "") String replicas,
			@RequestParam(defaultValue = "") String hostAndPorts
			) throws IOException {
		ValidationUtils.notBlank(replicas, "replicas");
		ValidationUtils.numeric(replicas, "replicas");

		ValidationUtils.notBlank(hostAndPorts, "hostAndPorts");

		return service.getCreateClusterParam(Integer.valueOf(replicas), Lists.newArrayList(StringUtils.split(hostAndPorts, ",")));
	}
}
