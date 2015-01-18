package com.behase.relumin.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.Data;

@Data
public class SlotInfo {
	private int startSlotNumber;
	private int endSlotNumber;
	private Map<String, String> master = Maps.newLinkedHashMap();
	private List<Map<String, String>> slaves = Lists.newArrayList();
}
