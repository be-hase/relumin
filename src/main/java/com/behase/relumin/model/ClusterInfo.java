package com.behase.relumin.model;

import lombok.Data;

@Data
public class ClusterInfo {
	private boolean ok;
	private int slotsAssigned;
	private int slotsOk;
	private int slotsPfail;
	private int slotsFail;
	private int knownNodes;
	private int size;
	private int currentEpoch;
	private long statsMessagesSent;
	private long statsMessagesReceived;
}
