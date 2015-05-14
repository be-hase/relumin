package com.behase.relumin.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.support.RedisTrib;
import com.behase.relumin.util.JedisUtils;
import com.behase.relumin.util.ValidationUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RedisTribServiceImpl implements RedisTribService {

	@Override
	public List<CreateClusterParam> getCreateClusterParams(int replicas, List<String> hostAndPortRanges)
			throws IOException {
		ValidationUtils.replicas(replicas);
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
}
