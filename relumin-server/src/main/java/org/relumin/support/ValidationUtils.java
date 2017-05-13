package org.relumin.support;

import java.security.InvalidParameterException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.relumin.model.CreateClusterParam;
import org.relumin.support.redis.RedisSupport;

import com.google.common.collect.Sets;

public final class ValidationUtils {
    public static void createClusterParams(final List<CreateClusterParam> params) {
        final Set<Integer> slots = Sets.newTreeSet();
        final Set<String> hostAndPorts = Sets.newHashSet();

        if (params.size() < 3) {
            throw new IllegalArgumentException("Redis Cluster requires at least 3 master nodes.");
        }

        params.forEach(param -> {
            slotNumber(param.getStartSlotNumber());
            slotNumber(param.getEndSlotNumber());
            final int start = Integer.valueOf(param.getStartSlotNumber());
            final int end = Integer.valueOf(param.getEndSlotNumber());
            for (int i = start; i <= end; i++) {
                if (slots.contains(i)) {
                    throw new IllegalArgumentException(String.format("Duplicate slot number(%s).", i));
                }
                slots.add(i);
            }

            hostAndPort(param.getMaster());
            if (hostAndPorts.contains(param.getMaster())) {
                throw new IllegalArgumentException(
                        String.format("Duplicate hostAndPort(%s).", param.getMaster()));
            }
            hostAndPorts.add(param.getMaster());

            param.getReplicas().forEach(replica -> {
                hostAndPort(replica);
                if (hostAndPorts.contains(replica)) {
                    throw new IllegalArgumentException(String.format("Duplicate hostAndPort(%s).", replica));
                }
                hostAndPorts.add(replica);
            });
        });

        final int minSlot = slots.stream().min(Integer::compare).get();
        final int maxSlot = slots.stream().max(Integer::compare).get();
        if (minSlot != 0
            || maxSlot != RedisSupport.ALL_SLOTS_SIZE - 1
            || slots.size() != RedisSupport.ALL_SLOTS_SIZE) {
            Set<Integer> allSlots = Sets.newTreeSet();
            for (int i = 0; i < RedisSupport.ALL_SLOTS_SIZE; i++) {
                allSlots.add(i);
            }
            allSlots.removeAll(slots);
            throw new IllegalArgumentException(String.format("Slot is not enough. You must specify %s.",
                                                             new RedisSupport().slotsDisplay(allSlots)));
        }
    }

    public static void hostAndPort(final String hostAndPort) {
        final String[] hostAndPortArray = StringUtils.split(hostAndPort, ":");
        if (hostAndPortArray.length != 2) {
            throw new IllegalArgumentException(
                    String.format("HostAndPort is invalid format. (%s)", hostAndPort));
        }
        try {
            Integer.valueOf(hostAndPortArray[1]);
        } catch (Exception ignored) {
            throw new IllegalArgumentException(
                    String.format("Port is invalid. (%s)", hostAndPortArray[1]));
        }
    }

    public static void slotNumber(final String slot) {
        final int slotInt;
        try {
            slotInt = Integer.valueOf(slot);
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException(String.format("Slot number must be numeric. (%s)", slot));
        }
        slotNumber(slotInt);
    }

    public static void slotNumber(final int slot) {
        if (0 <= slot && slot <= RedisSupport.ALL_SLOTS_SIZE - 1) {
            // ok
        } else {
            throw new IllegalArgumentException(
                    String.format("Slot number must be in range '0 - 16383'. (%s)", slot));
        }
    }

    public static void slotCount(final int slotCount) {
        if (1 <= slotCount && slotCount <= RedisSupport.ALL_SLOTS_SIZE) {
            // ok
        } else {
            throw new InvalidParameterException(
                    String.format("Slot count must be in range '1 - 16384'. (%s)", slotCount));
        }
    }

    public static void replicas(final int replicas) {
        if (replicas < 0) {
            throw new IllegalArgumentException(
                    String.format("Replicas must be equal or longer than 0. (%s)", replicas));
        }
    }

    public static void notBlank(final String str, final String paramName) {
        if (StringUtils.isBlank(str)) {
            throw new InvalidParameterException(String.format("%s must not be blank.", paramName));
        }
    }

    private ValidationUtils() {
    }
}
