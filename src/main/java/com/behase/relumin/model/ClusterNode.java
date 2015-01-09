package com.behase.relumin.model;

import lombok.Data;

@Data
public class ClusterNode {
	private String nodeId;
	private String hostAndPort;
	private boolean master;
	private boolean slave;
	private boolean fail;
	private String masterNodeId;
	private long timeLastPing;
	private long timeLastPong;
	private long epoch;
	private boolean connect;
	private String servedSlots;
}
