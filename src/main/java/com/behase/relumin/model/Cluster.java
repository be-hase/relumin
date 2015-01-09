package com.behase.relumin.model;

import java.util.List;

import lombok.Data;

@Data
public class Cluster {
	private ClusterInfo info;
	private List<ClusterNode> nodes;
}
