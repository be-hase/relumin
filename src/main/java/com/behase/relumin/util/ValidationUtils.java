package com.behase.relumin.util;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.behase.relumin.Constants;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.param.CreateClusterParam;
import com.google.common.collect.Sets;

public class ValidationUtils {
	private ValidationUtils() {
	}

	private static final String NAME_REGEX = "^[a-zA-Z0-9_-]{1,20}$";

	public static void clusterName(String name) {
		if (!name.matches(NAME_REGEX)) {
			throw new InvalidParameterException(String.format("clusterName is invalid. (%s)", name));
		}
	}

	public static void hostAndPort(String node) {
		String[] hostAndPortArray = StringUtils.split(node, ":");
		if (hostAndPortArray.length != 2) {
			throw new InvalidParameterException(String.format("Node is invalid. (%s)", node));
		}
		try {
			Integer.valueOf(hostAndPortArray[1]);
		} catch (Exception e) {
			throw new InvalidParameterException(String.format("Node's port is invalid. (%s)", hostAndPortArray[1]));
		}
	}

	public static void slotNumber(String slot) {
		int slotInt;
		try {
			slotInt = Integer.valueOf(slot);
		} catch (NumberFormatException e) {
			throw new InvalidParameterException(String.format("Slot number must be numeric. (%s)", slot));
		}
		slotNumber(slotInt);
	}

	public static void slotNumber(int slot) {
		if (slot < 0) {
			throw new InvalidParameterException(String.format("Slot number must be in range '0 - 16383'. (%s)", slot));
		}
		if (slot > Constants.ALL_SLOTS_SIZE - 1) {
			throw new InvalidParameterException(String.format("Slot number must be in range '0 - 16383'. (%s)", slot));
		}
	}

	public static void slotCount(int slotCount) {
		if (slotCount <= 0 || Constants.ALL_SLOTS_SIZE < slotCount) {
			throw new InvalidParameterException(String.format("Slot count must be in range '1 - 16384'. (%s)", slotCount));
		}
	}

	public static void hostAndPortRange(String node) {
		String[] hostAndPortRangeArray = StringUtils.split(node, ":");
		if (hostAndPortRangeArray.length != 2) {
			throw new InvalidParameterException(String.format("Node is invalid. (%s)", node));
		}
		String[] portRange = StringUtils.split(hostAndPortRangeArray[1], "-");
		for (String port : portRange) {
			try {
				Integer.valueOf(port);
			} catch (Exception e) {
				throw new InvalidParameterException(String.format("Node's port is invalid. (%s)", port));
			}
		}
	}

	public static void replicas(int replicas) {
		if (replicas < 0) {
			throw new InvalidParameterException(String.format("Replicas must be equal or longer than 0. (%s)", replicas));
		}
	}

	public static void notBlank(String str, String paramName) {
		if (StringUtils.isBlank(str)) {
			throw new InvalidParameterException(String.format("%s must be not blank.", paramName));
		}
	}

	public static void numeric(String str, String paramName) {
		if (!StringUtils.isNumeric(str)) {
			throw new InvalidParameterException(String.format("%s must be numeric. (%s)", paramName, str));
		}
	}

	public static void number(String str, String paramName) {
		try {
			Integer.valueOf(str);
		} catch (Exception e) {
			throw new InvalidParameterException(String.format("%s must be number. (%s)", paramName, str));
		}
	}

	public static void createClusterParams(List<CreateClusterParam> params) {
		Set<Integer> slots = Sets.newTreeSet();
		Set<String> hostAndPorts = Sets.newHashSet();

		if (params.size() < 3) {
			throw new InvalidParameterException("Redis Cluster requires at least 3 master nodes.");
		}

		params.forEach(param -> {
			ValidationUtils.slotNumber(param.getStartSlotNumber());
			ValidationUtils.slotNumber(param.getEndSlotNumber());
			int start = Integer.valueOf(param.getStartSlotNumber());
			int end = Integer.valueOf(param.getEndSlotNumber());
			for (int i = start; i <= end; i++) {
				if (slots.contains(i)) {
					throw new InvalidParameterException(String.format("Duplicate slot number(%s).", i));
				}
				slots.add(i);
			}

			ValidationUtils.hostAndPort(param.getMaster());
			if (hostAndPorts.contains(param.getMaster())) {
				throw new InvalidParameterException(String.format("Duplicate hostAndPort(%s).", param.getMaster()));
			}
			hostAndPorts.add(param.getMaster());

			param.getReplicas().forEach(replica -> {
				ValidationUtils.hostAndPort(replica);
				if (hostAndPorts.contains(replica)) {
					throw new InvalidParameterException(String.format("Duplicate hostAndPort(%s).", replica));
				}
				hostAndPorts.add(replica);
			});
		});

		int minSlot = slots.stream().min(Integer::compare).get();
		int maxSlot = slots.stream().max(Integer::compare).get();
		if (minSlot != 0 || maxSlot != (Constants.ALL_SLOTS_SIZE - 1) || slots.size() != Constants.ALL_SLOTS_SIZE) {
			Set<Integer> allSlots = Sets.newTreeSet();
			for (int i = 0; i < Constants.ALL_SLOTS_SIZE; i++) {
				allSlots.add(i);
			}
			allSlots.removeAll(slots);
			throw new InvalidParameterException(String.format("Slot is not enough. You must specify %s.", JedisUtils.slotsDisplay(allSlots)));
		}
	}
}
