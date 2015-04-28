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
import com.behase.relumin.service.NodeService;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

//@Slf4j
@RestController
@RequestMapping(value = "/api/cluster/{clusterName}")
public class NodeApiController {
	@Autowired
	NodeService nodeService;

	@RequestMapping(value = "/node/{nodeId}", method = RequestMethod.GET)
	public Object getClusterList(
			@PathVariable String clusterName,
			@PathVariable String nodeId,
			@RequestParam(defaultValue = "0") String start,
			@RequestParam(defaultValue = "-1") String end,
			@RequestParam(defaultValue = "") String fields
			) {
		long startLong;
		long endLong;
		try {
			startLong = Long.valueOf(start);
		} catch (Exception e) {
			throw new InvalidParameterException("'start' is must be number.");
		}
		try {
			endLong = Long.valueOf(end);
		} catch (Exception e) {
			throw new InvalidParameterException("'end' is must be number.");
		}

		List<String> fieldsList = Lists.newArrayList();
		if (StringUtils.isNotBlank(fields)) {
			fieldsList.addAll(Splitter.on(",").splitToList(fields));
		}
		return nodeService.getStaticsInfoHistory(clusterName, nodeId, startLong, endLong, fieldsList);
	}
}
