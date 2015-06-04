package com.behase.relumin.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.support.RedisTrib;
import com.behase.relumin.util.JedisUtils;

@Service
public class RedisTribServiceImpl implements RedisTribService {

	@Override
	public List<CreateClusterParam> getCreateClusterParams(int replicas, List<String> hostAndPortRanges)
			throws IOException {
		Set<String> hostAndPorts = JedisUtils.getHostAndPorts(hostAndPortRanges);

		try (RedisTrib trib = new RedisTrib()) {
			return trib.getCreateClusterParams(replicas, hostAndPorts);
		}
	}

	@Override
	public void createCluster(List<CreateClusterParam> params) throws Exception {
		try (RedisTrib trib = new RedisTrib()) {
			trib.createCluster(params);
		}
	}

	@Override
	public void reshardCluster(String hostAndPort, int slotCount, String fromNodeIds, String toNodeId) throws Exception {
		try (RedisTrib trib = new RedisTrib()) {
			trib.reshardCluster(hostAndPort, slotCount, fromNodeIds, toNodeId);
		}
	}

	@Override
	public List<String> checkCluster(String hostAndPort) throws Exception {
		try (RedisTrib trib = new RedisTrib()) {
			return trib.checkCluster(hostAndPort);
		}
	}

	@Override
	public void fixCluster(String hostAndPort) throws Exception {
		try (RedisTrib trib = new RedisTrib()) {
			trib.fixCluster(hostAndPort);
		}
	}

	@Override
	public void addNodeIntoCluster(String hostAndPort, String newHostAndPort, String masterNodeId) throws Exception {
		try (RedisTrib trib = new RedisTrib()) {
			if (StringUtils.isBlank(masterNodeId)) {
				trib.addNodeIntoCluster(hostAndPort, newHostAndPort);
			} else {
				trib.addNodeIntoClusterAsReplica(hostAndPort, newHostAndPort, masterNodeId);
			}
		}
	}

	@Override
	public void deleteNodeFromCluster(String hostAndPort, String nodeId, String reset, boolean shutdown)
			throws Exception {
		try (RedisTrib trib = new RedisTrib()) {
			trib.deleteNodeOfCluster(hostAndPort, nodeId, reset, shutdown);
		}
	}

	@Override
	public void replicateNode(String hostAndPort, String masterNodeId) throws Exception {
		try (RedisTrib trib = new RedisTrib()) {
			trib.replicateNode(hostAndPort, masterNodeId);
		}
	}

	@Override
	public void failoverNode(String hostAndPort) throws Exception {
		try (RedisTrib trib = new RedisTrib()) {
			trib.failoverNode(hostAndPort);
		}
	}
}
