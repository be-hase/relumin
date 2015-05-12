package com.behase.relumin.service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.support.RedisTrib;
import com.behase.relumin.util.ValidationUtils;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RedisTribServiceImpl implements RedisTribService {

	@Override
	public List<CreateClusterParam> getCreateClusterParam(int replicas, List<String> hostAndPortRanges)
			throws IOException {
		ValidationUtils.replicas(replicas);
		Set<String> hostAndPorts = buildHostAndPorts(hostAndPortRanges);

		try (RedisTrib trib = new RedisTrib()) {
			return trib.getCreateClusterParam(replicas, hostAndPorts);
		}
	}

	private Set<String> buildHostAndPorts(List<String> hostAndPortRanges) {
		Set<String> hostAndPorts = Sets.newTreeSet();

		hostAndPortRanges.forEach(v -> {
			ValidationUtils.hostAndPortRange(v);
			String[] hostAndPortRangeArray = StringUtils.split(v, ":");

			String[] portRangeArray = StringUtils.split(hostAndPortRangeArray[1], "-");
			int start = Integer.valueOf(portRangeArray[0]);
			int end;
			if (portRangeArray.length > 1) {
				end = Integer.valueOf(portRangeArray[1]);
			} else {
				end = start;
			}
			if (start > end) {
				throw new InvalidParameterException(String.format("%s is invalid. start port must be equal or less than end.", v));
			}
			for (int i = start; i <= end; i++) {
				hostAndPorts.add(new StringBuilder().append(hostAndPortRangeArray[0]).append(":").append(i).toString());
			}
		});

		return hostAndPorts;
	}
}
