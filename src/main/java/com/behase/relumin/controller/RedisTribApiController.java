package com.behase.relumin.controller;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.service.ClusterService;
import com.behase.relumin.service.RedisTribService;
import com.behase.relumin.util.ValidationUtils;
import com.google.common.collect.Lists;

@RestController
@RequestMapping(value = "/api/trib")
public class RedisTribApiController {
	@Autowired
	private RedisTribService redisTibService;

	private ClusterService clusterService;

	@RequestMapping(value = "/create/params", method = RequestMethod.GET)
	public List<CreateClusterParam> getCreateParameter(
			@RequestParam(defaultValue = "") String replicas,
			@RequestParam(defaultValue = "") String hostAndPorts
			) throws Exception {
		ValidationUtils.notBlank(replicas, "replicas");
		ValidationUtils.numeric(replicas, "replicas");

		ValidationUtils.notBlank(hostAndPorts, "hostAndPorts");

		return redisTibService.getCreateClusterParams(Integer.valueOf(replicas), Lists.newArrayList(StringUtils.split(hostAndPorts, ",")));
	}

	@RequestMapping(value = "/create", method = RequestMethod.POST)
	public Object createCluster(
			@PathVariable String clusterName,
			@RequestBody List<CreateClusterParam> params
			) throws Exception {
		redisTibService.createCluster(params);

		return "";
	}

	@RequestMapping(value = "/reshard", method = RequestMethod.POST)
	public Object reshardCluster(
			String hostAndPort,
			String slotCount,
			String fromNodeIds,
			String toNodeId
			) throws Exception {
		ValidationUtils.notBlank(slotCount, "slotCount");
		ValidationUtils.numeric(slotCount, "slotCount");

		redisTibService.reshardCluster(hostAndPort, Integer.valueOf(slotCount), fromNodeIds, toNodeId);

		return "";
	}
}
