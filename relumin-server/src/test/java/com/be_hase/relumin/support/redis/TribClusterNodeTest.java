package com.be_hase.relumin.support.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.rule.OutputCapture;

import com.be_hase.relumin.model.ClusterNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TribClusterNodeTest {
    @Spy
    private TribClusterNode target = new TribClusterNode("localhost:10000", "");

    @Mock
    private RedisClient redisClient;
    @Mock
    private StatefulRedisConnection<String, String> redisConnection;
    @Mock
    private RedisCommands<String, String> redisCommands;
    @Mock
    private RedisSupport redisSupport;

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Before
    public void init() {
        Whitebox.setInternalState(target, "redisSupport", redisSupport);

        when(target.getRedisClient()).thenReturn(redisClient);
        when(redisClient.connect()).thenReturn(redisConnection);
        when(redisConnection.sync()).thenReturn(redisCommands);
    }

    private void initConnect() {
        target.connect();
    }

    @Test
    public void constructor() {
        TribClusterNode result = new TribClusterNode("localhost:10000", "");
        assertThat(result.getNode().getHostAndPort()).isEqualTo("localhost:10000");
    }

    @Test
    public void constructor_invalid_host_and_port() {
        assertThatThrownBy(() -> new TribClusterNode("", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid IP or Port. Use IP:Port format");
    }

    @Test
    public void connect() {
        assertThat(target.getRedisCommands()).isNull();
        target.connect(true);
        assertThat(target.getRedisCommands()).isNotNull();
    }

    @Test
    public void connectAbortIsTrueAndCannotConnectRedis() {
        when(redisCommands.ping()).thenThrow(new RuntimeException());

        assertThatThrownBy(() -> target.connect(true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to connect to node(localhost:10000).");
    }

    @Test
    public void assertCluster() {
        initConnect();
        when(redisSupport.parseInfoResult(anyString())).thenReturn(ImmutableMap.of("cluster_enabled", "1"));

        target.assertCluster();
    }

    @Test
    public void assertClusterClusterEnabledIsBlank() {
        initConnect();
        when(redisSupport.parseInfoResult(anyString())).thenReturn(ImmutableMap.of());

        assertThatThrownBy(() -> target.assertCluster())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'localhost:10000' is not configured as a cluster node.");
    }

    @Test
    public void assertClusterClusterEnabledIsNot1() {
        initConnect();
        when(redisSupport.parseInfoResult(anyString())).thenReturn(ImmutableMap.of("cluster_enabled", "0"));

        assertThatThrownBy(() -> target.assertCluster())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'localhost:10000' is not configured as a cluster node.");
    }

    @Test
    public void assertEmpty() {
        initConnect();
        when(redisSupport.parseInfoResult(anyString())).thenReturn(ImmutableMap.of());
        when(redisSupport.parseClusterInfoResult(anyString())).thenReturn(
                ImmutableMap.of("cluster_known_nodes", "1"));

        // when
        target.assertEmpty();
    }

    @Test
    public void assertEmptyDb0IsNotNull() {
        initConnect();
        when(redisSupport.parseInfoResult(anyString())).thenReturn(ImmutableMap.of("db0", "aaa"));
        when(redisSupport.parseClusterInfoResult(anyString())).thenReturn(
                ImmutableMap.of("cluster_known_nodes", "1"));

        assertThatThrownBy(() -> target.assertEmpty())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "'localhost:10000' is not empty. Either the node already knows other nodes (check with CLUSTER NODES) or contains some key in database 0.");
    }

    @Test
    public void assertEmptyClusterKnownNodesIsNot1() {
        initConnect();
        when(redisSupport.parseInfoResult(anyString())).thenReturn(ImmutableMap.of());
        when(redisSupport.parseClusterInfoResult(anyString())).thenReturn(
                ImmutableMap.of("cluster_known_nodes", "2"));

        assertThatThrownBy(() -> target.assertEmpty())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "'localhost:10000' is not empty. Either the node already knows other nodes (check with CLUSTER NODES) or contains some key in database 0.");
    }

    @Test
    public void loadInfo_getFriends_true() {
        // given
        List<ClusterNode> nodes = Lists.newArrayList(
                ClusterNode.builder()
                           .nodeId("nodeId1")
                           .hostAndPort("localhost:10000")
                           .flags(Sets.newHashSet("myself"))
                           .build(),
                ClusterNode.builder()
                           .nodeId("nodeId2")
                           .hostAndPort("localhost:10001")
                           .flags(Sets.newHashSet())
                           .build()
        );

        initConnect();
        when(redisSupport.parseClusterNodesResult(anyString(), anyString())).thenReturn(nodes);
        when(redisSupport.parseClusterInfoResult(anyString()))
                .thenReturn(ImmutableMap.of("cluster_state", "ok"));

        target.loadInfo(true);
        assertThat(target.getNode().getNodeId()).isEqualTo("nodeId1");
        assertThat(target.getFriends().get(0).getNodeId()).isEqualTo("nodeId2");
        assertThat(target.getClusterInfo()).containsEntry("cluster_state", "ok");
    }

    @Test
    public void loadInfoGetFriendsFalse() {
        List<ClusterNode> nodes = Lists.newArrayList(
                ClusterNode.builder()
                           .nodeId("nodeId1")
                           .hostAndPort("localhost:10000")
                           .flags(Sets.newHashSet("myself"))
                           .build(),
                ClusterNode.builder()
                           .nodeId("nodeId2")
                           .hostAndPort("localhost:10001")
                           .flags(Sets.newHashSet())
                           .build()
        );

        initConnect();
        when(redisSupport.parseClusterNodesResult(anyString(), anyString())).thenReturn(nodes);

        target.loadInfo();
        assertThat(target.getNode().getNodeId()).isEqualTo("nodeId1");
        assertThat(target.getFriends()).isEmpty();
    }

    @Test
    public void addTmpSlots() {
        target.addTmpSlots(Lists.newArrayList());
        target.addTmpSlots(Lists.newArrayList(1, 2, 3));
        assertThat(target.getTmpSlots().size()).isEqualTo(3);
        assertThat(target.isDirty()).isEqualTo(true);
    }

    @Test
    public void setAsReplica() {
        target.setAsReplica("masterNodeId");
        assertThat(target.getNode().getMasterNodeId()).isEqualTo("masterNodeId");
        assertThat(target.isDirty()).isEqualTo(true);
    }

    @Test
    public void flushNodeConfigNodeIsMaster() {
        initConnect();
        target.addTmpSlots(Sets.newHashSet(1, 2, 3));

        target.flushNodeConfig();
        verify(redisCommands).clusterAddSlots(1, 2, 3);
        assertThat(target.getNode().getServedSlotsSet()).contains(1, 2, 3);
        assertThat(target.getTmpSlots()).isEmpty();
        assertThat(target.isDirty()).isFalse();
    }

    @Test
    public void flushNodeConfigNodeIsReplicaAndReplicateFail() {
        initConnect();
        target.setAsReplica("masterNodeId");
        when(redisCommands.clusterReplicate(anyString())).thenThrow(new RuntimeException());

        target.flushNodeConfig();
        verify(redisCommands).clusterReplicate("masterNodeId");
        assertThat(capture.toString()).contains("Replicate error");
        assertThat(target.isDirty()).isTrue();
    }

    @Test
    public void flushNodeConfigNodeIsReplica() {
        initConnect();
        target.setAsReplica("masterNodeId");

        target.flushNodeConfig();
        verify(redisCommands).clusterReplicate("masterNodeId");
        assertThat(target.isDirty()).isFalse();
    }

    @Test
    public void flushNodeConfigDirtyIsFalseThenIgnore() {
        Whitebox.setInternalState(target, "dirty", false);

        target.flushNodeConfig();
        verify(redisCommands, never()).clusterAddSlots(any());
        verify(redisCommands, never()).clusterReplicate(anyString());
    }

    @Test
    public void getConfigSignature() {
        String clusterNodesText = "" +
                                  "7893f01887835a6e19b09ff663909fced0744926 127.0.0.1:7001 myself,master - 0 0 1 connected 0-2000 2001-4094 4095 [93-<-292f8b365bb7edb5e285caf0b7e6ddc7265d2f4f] [77->-e7d1eecce10fd6bb5eb35b9f99a514335d9ba9ca]\n"
                                  +
                                  "9bd5a779d5981cee7d561dc2bfc984ffbfc744d3 10.128.214.37:7002 slave 4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2 0 1459242326643 5 disconnected\n"
                                  +
                                  "c3c0b2b7d7d50e339565de468e7ebd7db79a1ea5 10.128.214.37:7003 master - 0 1459242325640 3 connected 8192-12287\n"
                                  +
                                  "20e7c57506199c468b0672fda7b00d12a2d6a547 10.128.214.37:7004 slave a4f318b3fb0affd5d130b29cb6161a7e225216b5 0 1459242324639 6 connected\n"
                                  +
                                  "8e309bc36225a6bfd46ede7ff377b54e0bdbfc5d 10.128.214.38:7001 slave 7893f01887835a6e19b09ff663909fced0744926 0 1459242328644 1 connected\n"
                                  +
                                  "4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2 10.128.214.38:7002 master - 0 1459242323638 5 connected 4096-8191\n"
                                  +
                                  "7040f0339855ff0faf1abeb32baad0d6441e8e2f 10.128.214.38:7003 slave c3c0b2b7d7d50e339565de468e7ebd7db79a1ea5 0 1459242327643 3 connected\n"
                                  +
                                  "a4f318b3fb0affd5d130b29cb6161a7e225216b5 10.128.214.38:7004 master - 0 1459242328644 6 connected 12288-16383";

        initConnect();
        when(redisCommands.clusterNodes()).thenReturn(clusterNodesText);

        // when
        String result = target.getConfigSignature();
        String expected = "4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2:4096-8191|" +
                          "7893f01887835a6e19b09ff663909fced0744926:0-2000,2001-4094,4095|" +
                          "a4f318b3fb0affd5d130b29cb6161a7e225216b5:12288-16383|" +
                          "c3c0b2b7d7d50e339565de468e7ebd7db79a1ea5:8192-12287";
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void equalsTest() {
        // when
        TribClusterNode node = new TribClusterNode("localhost:10000", "");

        // given
        assertThat(node.equals(null)).isFalse();
        assertThat(node.equals("")).isFalse();
        assertThat(node.equals(node)).isTrue();
        assertThat(node.equals(new TribClusterNode("localhost:10000", ""))).isTrue();
        assertThat(node.equals(new TribClusterNode("localhost:10001", ""))).isFalse();
    }
}
