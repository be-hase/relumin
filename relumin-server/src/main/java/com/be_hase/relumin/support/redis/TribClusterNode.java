package com.be_hase.relumin.support.redis;

import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.be_hase.relumin.model.ClusterNode;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Almost clone of redis-trib.rb
 * https://github.com/antirez/redis/blob/unstable/src/redis-trib.rb
 */
@Slf4j
public class TribClusterNode implements AutoCloseable {
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> redisConnection;
    @Getter
    private RedisCommands<String, String> redisCommands;
    private final RedisSupport redisSupport = new RedisSupport();

    @Getter
    private ClusterNode node;
    @Getter
    private final String password;
    @Getter
    private Map<String, String> clusterInfo = Maps.newLinkedHashMap();
    @Getter
    private boolean dirty;
    @Getter
    private final List<ClusterNode> friends = Lists.newArrayList();
    @Getter
    private final Set<Integer> tmpSlots = Sets.newTreeSet();
    @Getter
    private final List<TribClusterNode> replicas = Lists.newArrayList();

    public TribClusterNode(final String hostAndPort, final String password) {
        final String[] hostAndPortArray = StringUtils.split(hostAndPort, ":");
        if (hostAndPortArray.length < 2) {
            throw new IllegalArgumentException("Invalid IP or Port. Use IP:Port format");
        }

        node = new ClusterNode();
        node.setHostAndPort(hostAndPort);
        this.password = password;
    }

    public boolean hasFlag(final String flag) {
        return node.getFlags().contains(flag);
    }

    public void connect() {
        connect(false);
    }

    public void connect(final boolean abort) {
        if (redisClient != null && redisConnection != null && redisCommands != null) {
            return;
        }

        try {
            final RedisURI.Builder redisURIBuilder = RedisURI.Builder.redis(node.getHost(), node.getPort());
            if (StringUtils.isNotBlank(password)) {
                redisURIBuilder.withPassword(password);
            }
            redisClient = RedisClient.create(redisURIBuilder.build());
            redisConnection = redisClient.connect();
            redisCommands = redisConnection.sync();

            redisCommands.ping();
        } catch (Exception e) {
            if (abort) {
                throw new IllegalArgumentException(
                        String.format("Failed to connect to node(%s).", node.getHostAndPort()), e);
            }

            redisClient = null;
            redisConnection = null;
            redisCommands = null;
        }
    }

    public void assertCluster() {
        final Map<String, String> infoResult = redisSupport.parseInfoResult(redisCommands.info());
        final String clusterEnabled = infoResult.get("cluster_enabled");
        if (StringUtils.isBlank(clusterEnabled) || StringUtils.equals(clusterEnabled, "0")) {
            throw new InvalidParameterException(
                    String.format("'%s' is not configured as a cluster node.", node.getHostAndPort()));
        }
    }

    public void assertEmpty() {
        final Map<String, String> infoResult = redisSupport.parseInfoResult(redisCommands.info());
        final Map<String, String> clusterInfoResult =
                redisSupport.parseClusterInfoResult(redisCommands.clusterInfo());

        if (infoResult.get("db0") != null
            || !StringUtils.equals(clusterInfoResult.get("cluster_known_nodes"), "1")) {
            throw new InvalidParameterException(
                    String.format(
                            "'%s' is not empty. Either the node already knows other nodes (check with CLUSTER NODES) "
                            + "or contains some key in database 0.",
                            node.getHostAndPort())
            );
        }
    }

    public void loadInfo() {
        loadInfo(false);
    }

    public void loadInfo(final boolean getFriends) {
        connect();

        final String hostAndPort = node.getHostAndPort();
        final List<ClusterNode> nodes =
                redisSupport.parseClusterNodesResult(redisCommands.clusterNodes(), hostAndPort);
        nodes.forEach(v -> {
            if (v.hasFlag("myself")) {
                node = v;
                node.setHostAndPort(hostAndPort);
                dirty = false;
                clusterInfo = redisSupport.parseClusterInfoResult(redisCommands.clusterInfo());
            } else {
                if (getFriends) {
                    friends.add(v);
                }
            }
        });
    }

    public void addTmpSlots(final Collection<Integer> slots) {
        tmpSlots.addAll(slots);
        dirty = true;
    }

    public void setAsReplica(final String masterNodeId) {
        node.setMasterNodeId(masterNodeId);
        dirty = true;
    }

    public void flushNodeConfig() {
        if (!dirty) {
            log.debug("Dirty is false, so ignore.");
            return;
        }

        if (StringUtils.isBlank(node.getMasterNodeId())) {
            log.debug("this node is master.");
            final int[] intArray = tmpSlots.stream().mapToInt(i -> i).toArray();
            node.getServedSlotsSet().addAll(tmpSlots);
            tmpSlots.clear();
            redisCommands.clusterAddSlots(intArray);
        } else {
            log.debug("this node is replica");
            try {
                redisCommands.clusterReplicate(node.getMasterNodeId());
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

    // Return a single string representing nodes and associated slots.
    // TODO: remove slaves from config when slaves will be handled
    // by Redis Cluster.
    public String getConfigSignature() {
        final List<String> config = Lists.newArrayList();

        final String result = redisCommands.clusterNodes();
        for (final String line : StringUtils.split(result, "\n")) {
            final String[] lineArray = StringUtils.split(line);

            List<String> slots = Stream.of(lineArray)
                                       .skip(8)
                                       .filter(v -> !StringUtils.startsWith(v, "["))
                                       .collect(Collectors.toList());

            if (!slots.isEmpty()) {
                Collections.sort(slots);
                config.add(Joiner.on(":").join(lineArray[0], Joiner.on(",").join(slots)));
            }
        }

        Collections.sort(config);
        return Joiner.on("|").join(config);
    }

    @Override
    public void close() {
        if (redisConnection != null) {
            redisConnection.close();
        }
        if (redisClient != null) {
            redisClient.shutdown();
        }

        redisClient = null;
        redisConnection = null;
        redisCommands = null;
    }

    @Override
    public String toString() {
        return node.getHostAndPort();
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
        return StringUtils.equalsIgnoreCase(node.getHostAndPort(), other.node.getHostAndPort());
    }
}
