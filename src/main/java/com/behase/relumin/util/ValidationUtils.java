package com.behase.relumin.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import com.behase.relumin.Constants;
import com.behase.relumin.exception.ApiException;

public class ValidationUtils {
	private ValidationUtils() {
	}

	private static final String NAME_REGEX = "^[a-zA-Z0-9_-]{1,20}$";

	public static void clusterName(String name) {
		if (!name.matches(NAME_REGEX)) {
			throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Name is invalid.", HttpStatus.BAD_REQUEST);
		}
	}

	public static void hostAndPort(String node) {
		String[] hostAndPortArray = StringUtils.split(node, ":");
		if (hostAndPortArray.length != 2) {
			throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Node is invalid.", HttpStatus.BAD_REQUEST);
		}
		try {
			Integer.valueOf(hostAndPortArray[1]);
		} catch (Exception e) {
			throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Node's port is invalid.", HttpStatus.BAD_REQUEST);
		}
	}

	public static void slotNumber(String slot) {
		int slotInt;
		try {
			slotInt = Integer.valueOf(slot);
		} catch (NumberFormatException e) {
			throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Slot number must be numeric value.", HttpStatus.BAD_REQUEST);
		}
		slotNumber(slotInt);
	}

	public static void slotNumber(int slot) {
		if (slot < 0) {
			throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Slot number must be in range '0 - 16383'.", HttpStatus.BAD_REQUEST);
		}
		if (slot > 16383) {
			throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Slot number must be in range '0 - 16383'.", HttpStatus.BAD_REQUEST);
		}
	}

	public static void hostAndPortRange(String node) {
		String[] hostAndPortRangeArray = StringUtils.split(node, ":");
		if (hostAndPortRangeArray.length != 2) {
			throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Node is invalid.", HttpStatus.BAD_REQUEST);
		}
		String[] portRange = StringUtils.split(hostAndPortRangeArray[1], "-");
		for (String port : portRange) {
			try {
				Integer.valueOf(port);
			} catch (Exception e) {
				throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Node's port is invalid.", HttpStatus.BAD_REQUEST);
			}
		}
	}

	public static void replicas(int replicas) {
		if (replicas < 0) {
			throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, "Replicas must be equal or longer than 0.", HttpStatus.BAD_REQUEST);
		}
	}

	public static void notBlank(String str, String paramName) {
		if (StringUtils.isBlank(str)) {
			throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, String.format("%s must be not blank.", paramName), HttpStatus.BAD_REQUEST);
		}
	}

	public static void numeric(String str, String paramName) {
		if (!StringUtils.isNumeric(str)) {
			throw new ApiException(Constants.ERR_CODE_INVALID_PARAMETER, String.format("%s must be numeric.", paramName), HttpStatus.BAD_REQUEST);
		}
	}
}
