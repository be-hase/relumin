package com.behase.relumin.controller;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.behase.relumin.model.Cluster;
import com.behase.relumin.service.ClusterService;
import com.google.common.collect.Maps;

//@Slf4j
@RestController
@RequestMapping(value = "/api")
public class ClusterApiController {
	@Autowired
	ClusterService clusterService;

	@RequestMapping(value = "/clusters", method = RequestMethod.GET)
	public Set<String> getClusterList() {
		return clusterService.getClusters();
	}

	@RequestMapping(value = "/cluster/{clusterName}", method = RequestMethod.GET)
	public Cluster getCluster(
			@PathVariable String clusterName
			) throws IOException {
		return clusterService.getCluster(clusterName);
	}

	@RequestMapping(value = "/cluster/{clusterName}", method = RequestMethod.POST)
	public Cluster setCluster(
			@PathVariable String clusterName,
			@RequestParam String hostAndPort
			) throws IOException {
		clusterService.setCluster(clusterName, hostAndPort);
		return clusterService.getCluster(clusterName);
	}

	@RequestMapping(value = "/cluster/{clusterName}", method = RequestMethod.DELETE)
	public Map<String, Boolean> deleteCluster(
			@PathVariable String clusterName
			) {
		clusterService.deleteCluster(clusterName);

		Map<String, Boolean> result = Maps.newHashMap();
		result.put("isSuccess", true);
		return result;
	}
}
