package com.behase.relumin.controller;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.service.ClusterService;
import com.behase.relumin.service.RedisTribService;
import com.behase.relumin.util.ValidationUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@RestController
@RequestMapping(value = "/api/trib")
public class RedisTribApiController {
	@Autowired
	private RedisTribService redisTibService;

	@Autowired
	private ClusterService clusterService;

	@Autowired
	private ObjectMapper mapper;

	@RequestMapping(value = "/create/params", method = RequestMethod.GET)
	public List<CreateClusterParam> getCreateParameter(
			@RequestParam(defaultValue = "") String replicas,
			@RequestParam(defaultValue = "") String hostAndPorts
			) throws Exception {
		ValidationUtils.number(replicas, "replicas");
		ValidationUtils.notBlank(hostAndPorts, "hostAndPorts");

		return redisTibService.getCreateClusterParams(Integer.valueOf(replicas), Lists.newArrayList(StringUtils.split(hostAndPorts, ",")));
	}

	@RequestMapping(value = "/create/{clusterName}", method = RequestMethod.POST)
	public Object createCluster(
			@PathVariable String clusterName,
			@RequestParam(defaultValue = "") String params
			) throws Exception {
		if (clusterService.existsClusterName(clusterName)) {
			throw new InvalidParameterException(String.format("This clusterName(%s) already exists.", clusterName));
		}

		List<CreateClusterParam> paramsList;
		try {
			paramsList = mapper.readValue(params, new TypeReference<List<CreateClusterParam>>() {
			});
		} catch (Exception e) {
			throw new InvalidParameterException("params is not JSON.");
		}
		redisTibService.createCluster(paramsList);
		clusterService.setCluster(clusterName, paramsList.get(0).getMaster());
		return clusterService.getCluster(clusterName);
	}

	@RequestMapping(value = "/create", method = RequestMethod.POST)
	public Object createCluster(
			@RequestParam(defaultValue = "") String params
			) throws Exception {
		List<CreateClusterParam> paramsList;
		try {
			paramsList = mapper.readValue(params, new TypeReference<List<CreateClusterParam>>() {
			});
		} catch (Exception e) {
			throw new InvalidParameterException("params is not JSON.");
		}
		redisTibService.createCluster(paramsList);
		return clusterService.getClusterByHostAndPort(paramsList.get(0).getMaster());
	}

	@RequestMapping(value = "/check", method = RequestMethod.GET)
	public Object checkCluster(
			@RequestParam(defaultValue = "") String hostAndPort
			) throws Exception {
		return ImmutableMap.of("errors", redisTibService.checkCluster(hostAndPort));
	}

	@RequestMapping(value = "/fix", method = RequestMethod.GET)
	public Object fixCluster(
			@RequestParam(defaultValue = "") String hostAndPort
			) throws Exception {
		redisTibService.fixCluster(hostAndPort);
		return ImmutableMap.of("errors", redisTibService.checkCluster(hostAndPort));
	}

	@RequestMapping(value = "/reshard", method = RequestMethod.POST)
	public Object reshardCluster(
			@RequestParam(defaultValue = "") String hostAndPort,
			@RequestParam(defaultValue = "") String slotCount,
			@RequestParam(defaultValue = "") String fromNodeIds,
			@RequestParam(defaultValue = "") String toNodeId
			) throws Exception {
		redisTibService.reshardCluster(hostAndPort, Integer.valueOf(slotCount), fromNodeIds, toNodeId);
		return clusterService.getClusterByHostAndPort(hostAndPort);
	}

	@RequestMapping(value = "/add-node", method = RequestMethod.POST)
	public Object addNode(
			@RequestParam(defaultValue = "") String hostAndPort,
			@RequestParam(defaultValue = "") String newHostAndPort,
			@RequestParam(defaultValue = "") String masterNodeId
			) throws Exception {
		redisTibService.addNodeIntoCluster(hostAndPort, newHostAndPort, masterNodeId);
		return clusterService.getClusterByHostAndPort(hostAndPort);
	}

	@RequestMapping(value = "/delete-node", method = RequestMethod.POST)
	public Object deleteNode(
			@RequestParam(defaultValue = "") String hostAndPort,
			@RequestParam(defaultValue = "") String nodeId,
			@RequestParam(defaultValue = "") String reset,
			@RequestParam(defaultValue = "") String shutdown
			) throws Exception {
		boolean shutdownBool = false;
		try {
			shutdownBool = Boolean.valueOf(shutdown);
		} catch (Exception e) {
		}

		redisTibService.deleteNodeFromCluster(hostAndPort, nodeId, reset, shutdownBool);
		return clusterService.getClusterByHostAndPort(hostAndPort);
	}

	@RequestMapping(value = "/replicate", method = RequestMethod.POST)
	public Object replicateNode(
			@RequestParam(defaultValue = "") String hostAndPort,
			@RequestParam(defaultValue = "") String masterNodeId
			) throws Exception {
		redisTibService.replicateNode(hostAndPort, masterNodeId);
		return clusterService.getClusterByHostAndPort(hostAndPort);
	}

	@RequestMapping(value = "/failover", method = RequestMethod.POST)
	public Object failoverNode(
			@RequestParam(defaultValue = "") String hostAndPort
			) throws Exception {
		redisTibService.failoverNode(hostAndPort);
		return clusterService.getClusterByHostAndPort(hostAndPort);
	}

}
