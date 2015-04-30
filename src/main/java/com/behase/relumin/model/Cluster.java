package com.behase.relumin.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class Cluster {
	private Map<String, String> info;
	private List<ClusterNode> nodes;
	private List<SlotInfo> slots;
}
