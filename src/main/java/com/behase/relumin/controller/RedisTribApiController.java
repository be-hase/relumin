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
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.service.ClusterService;
import com.behase.relumin.service.NodeService;
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
	private NodeService nodeService;

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
			@RequestParam(defaultValue = "") String clusterName
			) throws Exception {
		ClusterNode node = clusterService.getActiveClusterNode(clusterName);
		return ImmutableMap.of("errors", redisTibService.checkCluster(node.getHostAndPort()));
	}

	@RequestMapping(value = "/fix", method = RequestMethod.GET)
	public Object fixCluster(
			@RequestParam(defaultValue = "") String clusterName
			) throws Exception {
		ClusterNode node = clusterService.getActiveClusterNode(clusterName);
		redisTibService.fixCluster(node.getHostAndPort());
		return ImmutableMap.of("errors", redisTibService.checkCluster(node.getHostAndPort()));
	}

	@RequestMapping(value = "/reshard", method = RequestMethod.POST)
	public Object reshardCluster(
			@RequestParam(defaultValue = "") String clusterName,
			@RequestParam(defaultValue = "") String slotCount,
			@RequestParam(defaultValue = "") String fromNodeIds,
			@RequestParam(defaultValue = "") String toNodeId
			) throws Exception {
		ClusterNode node = clusterService.getActiveClusterNode(clusterName);
		redisTibService.reshardCluster(node.getHostAndPort(), Integer.valueOf(slotCount), fromNodeIds, toNodeId);
		return clusterService.getClusterByHostAndPort(node.getHostAndPort());
	}

	@RequestMapping(value = "/reshard-by-slots", method = RequestMethod.POST)
	public Object reshardClusterBySlots(
			@RequestParam(defaultValue = "") String clusterName,
			@RequestParam(defaultValue = "") String slots,
			@RequestParam(defaultValue = "") String toNodeId
			) throws Exception {
		ClusterNode node = clusterService.getActiveClusterNode(clusterName);
		redisTibService.reshardClusterBySlots(node.getHostAndPort(), Lists.newArrayList(StringUtils.split(slots, ",")), toNodeId);
		return clusterService.getClusterByHostAndPort(node.getHostAndPort());
	}

	@RequestMapping(value = "/add-node", method = RequestMethod.POST)
	public Object addNode(
			@RequestParam(defaultValue = "") String clusterName,
			@RequestParam(defaultValue = "") String hostAndPort,
			@RequestParam(defaultValue = "") String masterNodeId
			) throws Exception {
		ClusterNode node = clusterService.getActiveClusterNode(clusterName);
		redisTibService.addNodeIntoCluster(node.getHostAndPort(), hostAndPort, masterNodeId);
		return clusterService.getClusterByHostAndPort(hostAndPort);
	}

	@RequestMapping(value = "/delete-node", method = RequestMethod.POST)
	public Object deleteNode(
			@RequestParam(defaultValue = "") String clusterName,
			@RequestParam(defaultValue = "") String nodeId,
			@RequestParam(defaultValue = "") String isFail,
			@RequestParam(defaultValue = "") String reset,
			@RequestParam(defaultValue = "") String shutdown
			) throws Exception {
		boolean isFailBool = false;
		try {
			isFailBool = Boolean.valueOf(isFail);
		} catch (Exception e) {
		}
		if (isFailBool) {
			ClusterNode node = clusterService.getActiveClusterNodeWithExcludeNodeId(clusterName, nodeId);
			redisTibService.deleteFailNodeFromCluster(node.getHostAndPort(), nodeId);
			clusterService.setCluster(clusterName, node.getHostAndPort());
			return clusterService.getClusterByHostAndPort(node.getHostAndPort());
		}

		boolean shutdownBool = false;
		try {
			shutdownBool = Boolean.valueOf(shutdown);
		} catch (Exception e) {
		}

		ClusterNode node = clusterService.getActiveClusterNodeWithExcludeNodeId(clusterName, nodeId);
		redisTibService.deleteNodeFromCluster(node.getHostAndPort(), nodeId, reset, shutdownBool);
		clusterService.setCluster(clusterName, node.getHostAndPort());
		return clusterService.getClusterByHostAndPort(node.getHostAndPort());
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

	@RequestMapping(value = "/shutdown", method = RequestMethod.POST)
	public Object shutdown(
			@RequestParam(defaultValue = "") String clusterName,
			@RequestParam(defaultValue = "") String hostAndPort
			) throws Exception {
		ClusterNode node = clusterService.getActiveClusterNodeWithExcludeHostAndPort(clusterName, hostAndPort);
		nodeService.shutdown(hostAndPort);
		return clusterService.getClusterByHostAndPort(node.getHostAndPort());
	}
}
