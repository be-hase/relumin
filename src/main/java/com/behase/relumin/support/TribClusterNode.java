package com.behase.relumin.support;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.ClusterNode;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import redis.clients.jedis.Jedis;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Clone of redis-trib.rb
 *
 * @author Ryosuke Hasebe
 */
@Slf4j
public class TribClusterNode implements Closeable {
    private Jedis jedis;
    private ClusterNode info;
    private boolean dirty = false;
    private List<ClusterNode> friends = Lists.newArrayList();
    private Set<Integer> tmpSlots = Sets.newTreeSet();
    private List<TribClusterNode> replicas = Lists.newArrayList();

    public TribClusterNode(String hostAndPort) {
        String[] hostAndPortArray = StringUtils.split(hostAndPort, ":");
        checkArgument(hostAndPortArray.length >= 2, "Invalid IP or Port. Use IP:Port format");

        info = new ClusterNode();
        info.setHostAndPort(hostAndPort);
    }

    public Jedis getJedis() {
        return jedis;
    }

    public ClusterNode getInfo() {
        return info;
    }

    public boolean isDirty() {
        return dirty;
    }

    public List<ClusterNode> getFriends() {
        return friends;
    }

    public Set<Integer> getTmpServedSlots() {
        return tmpSlots;
    }

    public List<TribClusterNode> getReplicas() {
        return replicas;
    }

    public String getServedSlots() {
        return info.getServedSlots();
    }

    public boolean hasFlag(String flag) {
        return info.getFlags().contains(flag);
    }

    public void connect() {
        connect(false);
    }

    public void connect(boolean abort) {
        if (jedis != null) {
            return;
        }

        try {
            jedis = createJedisSupport().getJedisByHostAndPort(info.getHostAndPort());
            if (!"PONG".equalsIgnoreCase(jedis.ping())) {
                throw new InvalidParameterException(String.format("Invalid PONG-message from Redis(%s).", info.getHostAndPort()));
            }
        } catch (Exception e) {
            if (abort) {
                throw new InvalidParameterException(String.format("Failed to connect to node(%s).", info.getHostAndPort()));
            }
            jedis = null;
        }
    }

    public void assertCluster() {
        Map<String, String> infoResult = createJedisSupport().parseInfoResult(jedis.info());
        String clusterEnabled = infoResult.get("cluster_enabled");
        if (StringUtils.isBlank(clusterEnabled) || StringUtils.equals(clusterEnabled, "0")) {
            throw new InvalidParameterException(String.format("%s is not configured as a cluster node.", info.getHostAndPort()));
        }
    }

    public void assertEmpty() {
        Map<String, String> infoResult = createJedisSupport().parseInfoResult(jedis.info());
        Map<String, String> clusterInfoResult = createJedisSupport().parseClusterInfoResult(jedis.clusterInfo());
        if (infoResult.get("db0") != null || !StringUtils.equals(clusterInfoResult.get("cluster_known_nodes"), "1")) {
            throw new InvalidParameterException(
                    String.format(
                            "%s is not empty. Either the node already knows other nodes (check with CLUSTER NODES) " +
                                    "or contains some key in database 0.",
                            info.getHostAndPort()
                    )
            );
        }
    }

    public void loadInfo() {
        loadInfo(false);
    }

    public void loadInfo(boolean getFriend) {
        connect();

        String hostAndPort = info.getHostAndPort();
        List<ClusterNode> nodes = createJedisSupport().parseClusterNodesResult(jedis.clusterNodes(), hostAndPort);
        nodes.forEach(v -> {
            if (v.hasFlag("myself")) {
                info = v;
                info.setHostAndPort(hostAndPort);
            } else {
                if (getFriend) {
                    friends.add(v);
                }
            }
        });
    }

    public void addTmpSlots(Collection<Integer> slots) {
        if (slots.isEmpty()) {
            return;
        }
        tmpSlots.addAll(slots);
        dirty = true;
    }

    public void setAsReplica(String masterNodeId) {
        info.setMasterNodeId(masterNodeId);
        dirty = true;
    }

    public void flushNodeConfig() {
        if (!dirty) {
            return;
        }
        if (StringUtils.isBlank(info.getMasterNodeId())) {
            log.debug("this node is master. {}", getTmpServedSlots());
            int[] intArray = new int[tmpSlots.size()];
            int i = 0;
            for (Integer item : tmpSlots) {
                intArray[i] = item;
                i++;
            }
            jedis.clusterAddSlots(intArray);
            info.getServedSlotsSet().addAll(tmpSlots);
            tmpSlots.clear();
        } else {
            log.debug("this node is replica");
            try {
                jedis.clusterReplicate(info.getMasterNodeId());
            } catch (Exception e) {
                log.error("Replicate error.", e);
                // If the cluster did not already joined it is possible that
                // the slave does not know the master node yet. So on errors
                // we return ASAP leaving the dirty flag set, to flush the
                // config later.
                return;
            }
        }
        dirty = false;
    }

    public String getConfigSignature() {
        List<String> config = Lists.newArrayList();

        String result = jedis.clusterNodes();
        for (String line : StringUtils.split(result, "\n")) {
            String[] lineArray = StringUtils.split(line);

            List<String> slots = Lists.newArrayList();
            for (int i = 8; i < lineArray.length; i++) {
                slots.add(lineArray[i]);
            }
            slots.stream().filter(v -> !StringUtils.startsWith(v, "[")).collect(Collectors.toList());

            if (slots.size() > 0) {
                Collections.sort(slots);
                config.add(Joiner.on(":").join(lineArray[0], Joiner.on(",").join(slots)));
            }
        }

        Collections.sort(config);
        return Joiner.on("|").join(config);
    }

    JedisSupport createJedisSupport() {
        return new JedisSupport();
    }

    @Override
    public void close() throws IOException {
        if (jedis != null) {
            jedis.close();
        }
    }

    @Override
    public String toString() {
        return info.getHostAndPort();
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

        TribClusterNode other = (TribClusterNode) obj;
        return StringUtils.equalsIgnoreCase(getInfo().getHostAndPort(), other.getInfo().getHostAndPort());
    }
}
