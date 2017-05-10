package com.be_hase.relumin.model;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import com.be_hase.relumin.support.redis.RedisSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterNode {
    private String nodeId;
    private String hostAndPort;
    private Set<String> flags = Sets.newLinkedHashSet();
    private String masterNodeId;
    private long pingSent;
    private long pongReceived;
    private long configEpoch;
    private boolean connected;

    @JsonIgnore
    private Set<Integer> servedSlotsSet = Sets.newTreeSet();

    // key: slot number
    // val: nodeId
    private Map<Integer, String> migrating = Maps.newLinkedHashMap();
    private Map<Integer, String> importing = Maps.newLinkedHashMap();

    public boolean hasFlag(String flag) {
        if (CollectionUtils.isEmpty(flags)) {
            return false;
        }
        return flags.contains(flag);
    }

    public String getServedSlots() {
        return new RedisSupport().slotsDisplay(servedSlotsSet);
    }

    public int getSlotCount() {
        if (servedSlotsSet == null) {
            return 0;
        }
        return servedSlotsSet.size();
    }

    @JsonIgnore
    public String getHost() {
        return StringUtils.split(hostAndPort, ":")[0];
    }

    @JsonIgnore
    public int getPort() {
        return Integer.valueOf(StringUtils.split(hostAndPort, ":")[1]);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        ClusterNode other = (ClusterNode) obj;
        return StringUtils.equalsIgnoreCase(nodeId, other.nodeId);
    }
}
