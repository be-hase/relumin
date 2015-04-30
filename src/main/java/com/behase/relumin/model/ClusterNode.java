package com.behase.relumin.model;

import java.util.Map;
import java.util.Set;

import com.behase.relumin.util.JedisUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.Data;

@Data
public class ClusterNode {
	private String nodeId;
	private String hostAndPort;
	private Set<String> flags = Sets.newLinkedHashSet();
	private String masterNodeId;
	private long pingSent;
	private long pongReceived;
	private long configEpoch;
	private boolean connect;
	private Map<String, String> migration = Maps.newLinkedHashMap();
	private Map<String, String> importing = Maps.newLinkedHashMap();

	@JsonIgnore
	private Set<Integer> servedSlotsSet = Sets.newTreeSet();

	public boolean hasFlag(String flag) {
		if (flag == null || flags.isEmpty()) {
			return false;
		}
		return flags.contains(flag);
	}

	public String getServedSlots() {
		return JedisUtils.slotsDisplay(servedSlotsSet);
	}
}
