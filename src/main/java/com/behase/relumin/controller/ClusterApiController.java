package com.behase.relumin.controller;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.Notice;
import com.behase.relumin.model.PagerData;
import com.behase.relumin.model.SlowLog;
import com.behase.relumin.service.ClusterService;
import com.behase.relumin.service.LoggingOperationService;
import com.behase.relumin.service.NodeService;
import com.behase.relumin.util.ValidationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping(value = "/api")
public class ClusterApiController {
    @Autowired
    ClusterService clusterService;

    @Autowired
    NodeService nodeService;

    @Autowired
    private LoggingOperationService loggingOperationService;

    @Autowired
    ObjectMapper mapper;

    @RequestMapping(value = "/clusters", method = RequestMethod.GET)
    public Object getClusterList(
            @RequestParam(defaultValue = "") String full
    ) {
        Set<String> clusterNamesSet = clusterService.getClusters();
        List<String> clusterNames = Lists.newArrayList(clusterNamesSet);
        Collections.sort(clusterNames);

        if (StringUtils.equalsIgnoreCase(full, "true")) {
            List<Cluster> clusters = Lists.newArrayList();
            Map<String, Cluster> clustersMap = Maps.newHashMap();

            clusterNames.parallelStream().forEach(clusterName -> {
                try {
                    clustersMap.put(clusterName, clusterService.getCluster(clusterName));
                } catch (Exception e) {
                    log.error("Failed to get cluster. clusterName = {}", clusterName, e);
                }
            });
            clusterNames.forEach(clusterName -> {
                Cluster cluster = clustersMap.get(clusterName);
                if (cluster != null) {
                    clusters.add(cluster);
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
            Authentication authentication,
            @PathVariable String clusterName,
            @RequestParam String hostAndPort
    ) throws IOException {
        loggingOperationService.log("registerCluster", authentication, "clusterName={}, hostAndPort={}.", clusterName, hostAndPort);

        if (clusterService.existsClusterName(clusterName)) {
            throw new InvalidParameterException(String.format("This clusterName(%s) already exists.", clusterName));
        }
        clusterService.setCluster(clusterName, hostAndPort);
        return clusterService.getCluster(clusterName);
    }

    @RequestMapping(value = "/cluster/{clusterName}/change-cluster-name", method = RequestMethod.POST)
    public Cluster changeClusterName(
            Authentication authentication,
            @PathVariable String clusterName,
            @RequestParam String newClusterName
    ) throws IOException {
        loggingOperationService.log("changeClusterName", authentication, "clusterName={}, newClusterName={}.", clusterName, newClusterName);

        clusterService.changeClusterName(clusterName, newClusterName);

        return clusterService.getCluster(newClusterName);
    }

    @RequestMapping(value = "/cluster/{clusterName}/delete", method = RequestMethod.POST)
    public Map<String, Boolean> deleteClusterByPost(
            Authentication authentication,
            @PathVariable String clusterName
    ) {
        loggingOperationService.log("deleteCluster", authentication, "clusterName={}.", clusterName);

        clusterService.deleteCluster(clusterName);

        return ImmutableMap.of("isSuccess", true);
    }

    @RequestMapping(value = "/cluster/{clusterName}/metrics", method = {RequestMethod.GET, RequestMethod.POST})
    public Object getMetrics(
            @PathVariable String clusterName,
            @RequestParam(defaultValue = "") String nodes,
            @RequestParam(defaultValue = "") String fields,
            @RequestParam(defaultValue = "") String start,
            @RequestParam(defaultValue = "") String end
    ) {
        long startLong;
        long endLong;
        try {
            startLong = Long.valueOf(start);
        } catch (Exception e) {
            throw new InvalidParameterException("start must be number.");
        }
        try {
            endLong = Long.valueOf(end);
        } catch (Exception e) {
            throw new InvalidParameterException("end must be number.");
        }

        List<String> nodesList = Lists.newArrayList();
        if (StringUtils.isNotBlank(nodes)) {
            nodesList.addAll(Splitter.on(",").splitToList(nodes));
        }
        if (nodesList.isEmpty()) {
            throw new InvalidParameterException("nodes is empty.");
        }

        List<String> fieldsList = Lists.newArrayList();
        if (StringUtils.isNotBlank(fields)) {
            fieldsList.addAll(Splitter.on(",").splitToList(fields));
        }
        if (fieldsList.isEmpty()) {
            throw new InvalidParameterException("fields is empty.");
        }

        return clusterService.getClusterStaticsInfoHistory(clusterName, nodesList, fieldsList, startLong, endLong);
    }

    @RequestMapping(value = "/cluster/{clusterName}/notice", method = RequestMethod.GET)
    public Notice getClusterNotice(
            @PathVariable String clusterName
    ) throws IOException {
        Notice notice = clusterService.getClusterNotice(clusterName);
        if (notice == null) {
            return new Notice();
        }
        return notice;
    }

    @RequestMapping(value = "/cluster/{clusterName}/notice", method = RequestMethod.POST)
    public Notice setClusterNotice(
            Authentication authentication,
            @PathVariable String clusterName,
            @RequestParam(defaultValue = "") String notice
    ) throws Exception {
        loggingOperationService.log("addNotice", authentication, "clusterName={}, notice={}.", clusterName, notice);

        ValidationUtils.notBlank(notice, "notice");

        Notice noticeObj;
        try {
            noticeObj = mapper.readValue(notice, Notice.class);
        } catch (Exception e) {
            throw new InvalidParameterException("notice is invalid format.");
        }

        clusterService.setClusterNotice(clusterName, noticeObj);
        return clusterService.getClusterNotice(clusterName);
    }

    @RequestMapping(value = "/cluster/{clusterName}/slowlogs", method = {RequestMethod.GET})
    public PagerData<SlowLog> getSlowLogs(
            @PathVariable String clusterName,
            @RequestParam(defaultValue = "0") String offset,
            @RequestParam(defaultValue = "1000") String limit
    ) {
        long offsetLong;
        long limitLong;
        try {
            offsetLong = Long.valueOf(offset);
        } catch (Exception e) {
            throw new InvalidParameterException("offset must be number.");
        }
        try {
            limitLong = Long.valueOf(limit);
        } catch (Exception e) {
            throw new InvalidParameterException("limit must be number.");
        }

        return clusterService.getClusterSlowLogHistory(clusterName, offsetLong, limitLong);
    }
}
