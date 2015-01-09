package com.behase.relumin.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.behase.relumin.exception.ApiException;
import com.behase.relumin.service.ClusterService;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/cluster")
public class ClusterController {
	@Autowired
	ClusterService clusterService;

	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public Object getClusterList() {
		return clusterService.getClusters();
	}

	@RequestMapping(value = "/{clusterName}", method = RequestMethod.GET)
	public Object getCluster(
			@PathVariable String clusterName
			) throws ApiException, IOException {
		return clusterService.getCluster(clusterName);
	}

	@RequestMapping(value = "/{clusterName}", method = RequestMethod.POST)
	public Object setCluster(
			@PathVariable String clusterName,
			@RequestParam String hostAndPort
			) throws ApiException, IOException {
		clusterService.setCluster(clusterName, hostAndPort);
		return clusterService.getCluster(clusterName);
	}

	@RequestMapping(value = "/{clusterName}", method = RequestMethod.DELETE)
	public Object deleteCluster(
			@PathVariable String clusterName
			) throws ApiException {
		clusterService.deleteCluster(clusterName);

		Map<String, Boolean> result = Maps.newHashMap();
		result.put("isSuccess", true);
		return result;
	}
}
