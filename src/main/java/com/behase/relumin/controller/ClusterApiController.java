package com.behase.relumin.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.Cluster;
import com.behase.relumin.service.ClusterService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(value = "/api")
public class ClusterApiController {
	@Autowired
	ClusterService clusterService;

	@RequestMapping(value = "/clusters", method = RequestMethod.GET)
	public Object getClusterList(
			@RequestParam(defaultValue = "") String full
			) {
		Set<String> clusterNamesSet = clusterService.getClusters();
		List<String> clusterNames = Lists.newArrayList(clusterNamesSet);
		Collections.sort(clusterNames);

		if (StringUtils.equalsIgnoreCase(full, "true")) {
			List<Cluster> clusters = Lists.newArrayList();
			clusterNames.forEach(clusterName -> {
				try {
					clusters.add(clusterService.getCluster(clusterName));
				} catch (Exception e) {
					log.error("Failed to get cluster. clusterName = {}", clusterName, e);
				}
			});
			return clusters;
		} else {
			return clusterNames;
		}
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
		if (clusterService.existsClusterName(clusterName)) {
			throw new InvalidParameterException(String.format("This clusterName(%s) already exists.", clusterName));
		}
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
