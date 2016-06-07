package com.behase.relumin.support;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.ClusterNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.OutputCapture;
import redis.clients.jedis.Jedis;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TribClusterNodeTest {
    @Spy
    private TribClusterNode tested = new TribClusterNode("localhost:10000");

    @Mock
    private Jedis jedis;

    @Mock
    private JedisSupport jedisSupport;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Before
    public void init() {
        doReturn(jedisSupport).when(tested).createJedisSupport();
        doReturn(jedis).when(jedisSupport).getJedisByHostAndPort(anyString());
    }

    private void initConnect() {
        tested.connect();
    }

    @Test
    public void constructor_invalid_host_and_port() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Invalid IP or Port. Use IP:Port format"));

        // when
        tested = new TribClusterNode("");
    }

    @Test
    public void constructor() {
        // when
        tested = new TribClusterNode("localhost:10000");

        // then
        assertThat(tested.getNodeInfo().getHostAndPort(), is("localhost:10000"));
    }


    @Test
    public void connect_abort_is_true_and_cannot_connect_redis_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Failed to connect to node"));

        // given
        doThrow(Exception.class).when(jedis).ping();

        // when
        tested.connect(true);
    }

    @Test
    public void connect() throws Exception {
        // when
        tested.connect(true);

        // then
        assertThat(tested.getJedis(), is(not(nullValue())));
    }

    @Test
    public void assertCluster_clusterEnabled_is_blank_then_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("is not configured as a cluster node"));

        // given
        initConnect();
        doReturn(Maps.newHashMap()).when(jedisSupport).parseInfoResult(anyString());

        // when
        tested.assertCluster();
    }

    @Test
    public void assertCluster_clusterEnabled_is_not_1_then_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("is not configured as a cluster node"));

        // given
        initConnect();
        doReturn(ImmutableMap.of("cluster_enabled", "0")).when(jedisSupport).parseInfoResult(anyString());

        // when
        tested.assertCluster();
    }

    @Test
    public void assertEmpty_db0_is_not_null_then_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("is not empty. Either the node already knows other nodes"));

        // given
        initConnect();
        doReturn(ImmutableMap.of("db0", "aaa")).when(jedisSupport).parseInfoResult(anyString());
        doReturn(ImmutableMap.of("cluster_known_nodes", "1")).when(jedisSupport).parseClusterInfoResult(anyString());

        // when
        tested.assertEmpty();
    }

    @Test
    public void assertEmpty_cluster_known_nodes_is_not_1_then_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("is not empty. Either the node already knows other nodes"));

        // given
        initConnect();
        doReturn(Maps.newHashMap()).when(jedisSupport).parseInfoResult(anyString());
        doReturn(ImmutableMap.of("cluster_known_nodes", "0")).when(jedisSupport).parseClusterInfoResult(anyString());

        // when
        tested.assertEmpty();
    }

    @Test
    public void assertEmpty() {
        // given
        initConnect();
        doReturn(Maps.newHashMap()).when(jedisSupport).parseInfoResult(anyString());
        doReturn(ImmutableMap.of("cluster_known_nodes", "1")).when(jedisSupport).parseClusterInfoResult(anyString());

        // when
        tested.assertEmpty();
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
        doReturn(nodes).when(jedisSupport).parseClusterNodesResult(anyString(), anyString());
        doReturn(ImmutableMap.of("cluster_state", "ok")).when(jedisSupport).parseClusterInfoResult(anyString());

        // when
        tested.loadInfo(true);

        // then
        assertThat(tested.getNodeInfo().getNodeId(), is("nodeId1"));
        assertThat(tested.getFriends().get(0).getNodeId(), is("nodeId2"));
        assertThat(tested.getClusterInfo(), hasEntry("cluster_state", "ok"));
    }

    @Test
    public void loadInfo_getFriends_false() {
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
        doReturn(nodes).when(jedisSupport).parseClusterNodesResult(anyString(), anyString());

        // when
        tested.loadInfo();

        // then
        assertThat(tested.getNodeInfo().getNodeId(), is("nodeId1"));
        assertThat(tested.getFriends(), is(empty()));
    }

    @Test
    public void addTmpSlots() {
        // when
        tested.addTmpSlots(Lists.newArrayList());
        tested.addTmpSlots(Lists.newArrayList(1, 2, 3));

        // then
        assertThat(tested.getTmpSlots().size(), is(3));
        assertThat(tested.isDirty(), is(true));
    }

    @Test
    public void setAsReplica() {
        // when
        tested.setAsReplica("masterNodeId");

        // then
        assertThat(tested.getNodeInfo().getMasterNodeId(), is("masterNodeId"));
        assertThat(tested.isDirty(), is(true));
    }

    @Test
    public void flushNodeConfig_node_is_master() {
        // given
        initConnect();
        tested.addTmpSlots(Sets.newHashSet(3, 2, 1));

        // when
        tested.flushNodeConfig();

        // then
        verify(jedis).clusterAddSlots(new int[]{1, 2, 3});
        assertThat(tested.getNodeInfo().getServedSlotsSet(), contains(1, 2, 3));
        assertThat(tested.getTmpSlots(), is(empty()));
        assertThat(tested.isDirty(), is(false));
    }

    @Test
    public void flushNodeConfig_node_is_replica_and_replicate_fail() {
        // given
        initConnect();
        tested.setAsReplica("masterNodeId");
        doThrow(Exception.class).when(jedis).clusterReplicate(anyString());

        // when
        tested.flushNodeConfig();

        // then
        verify(jedis).clusterReplicate("masterNodeId");
        assertThat(capture.toString(), containsString("Replicate error"));
        assertThat(tested.isDirty(), is(true));
    }

    @Test
    public void flushNodeConfig_node_is_replica() {
        // given
        initConnect();
        tested.setAsReplica("masterNodeId");

        // when
        tested.flushNodeConfig();

        // then
        verify(jedis).clusterReplicate("masterNodeId");
        assertThat(tested.isDirty(), is(false));
    }

    @Test
    public void flushNodeConfig_dirty_is_false_then_ignore() {
        // given
        Whitebox.setInternalState(tested, "dirty", false);

        // when
        tested.flushNodeConfig();

        // then
        assertThat(capture.toString(), containsString("Dirty is false, so ignore"));
    }


    @Test
    public void getConfigSignature() {
        // given
        String clusterNodesText = "" +
                "7893f01887835a6e19b09ff663909fced0744926 127.0.0.1:7001 myself,master - 0 0 1 connected 0-2000 2001-4094 4095 [93-<-292f8b365bb7edb5e285caf0b7e6ddc7265d2f4f] [77->-e7d1eecce10fd6bb5eb35b9f99a514335d9ba9ca]\n" +
                "9bd5a779d5981cee7d561dc2bfc984ffbfc744d3 10.128.214.37:7002 slave 4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2 0 1459242326643 5 disconnected\n" +
                "c3c0b2b7d7d50e339565de468e7ebd7db79a1ea5 10.128.214.37:7003 master - 0 1459242325640 3 connected 8192-12287\n" +
                "20e7c57506199c468b0672fda7b00d12a2d6a547 10.128.214.37:7004 slave a4f318b3fb0affd5d130b29cb6161a7e225216b5 0 1459242324639 6 connected\n" +
                "8e309bc36225a6bfd46ede7ff377b54e0bdbfc5d 10.128.214.38:7001 slave 7893f01887835a6e19b09ff663909fced0744926 0 1459242328644 1 connected\n" +
                "4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2 10.128.214.38:7002 master - 0 1459242323638 5 connected 4096-8191\n" +
                "7040f0339855ff0faf1abeb32baad0d6441e8e2f 10.128.214.38:7003 slave c3c0b2b7d7d50e339565de468e7ebd7db79a1ea5 0 1459242327643 3 connected\n" +
                "a4f318b3fb0affd5d130b29cb6161a7e225216b5 10.128.214.38:7004 master - 0 1459242328644 6 connected 12288-16383";

        initConnect();
        doReturn(clusterNodesText).when(jedis).clusterNodes();

        // when
        String result = tested.getConfigSignature();

        // then
        String expected = "4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2:4096-8191|" +
                "7893f01887835a6e19b09ff663909fced0744926:0-2000,2001-4094,4095|" +
                "a4f318b3fb0affd5d130b29cb6161a7e225216b5:12288-16383|" +
                "c3c0b2b7d7d50e339565de468e7ebd7db79a1ea5:8192-12287";
        assertThat(result, is(expected));
    }

    @Test
    public void equals_test() {
        // when
        tested = new TribClusterNode("localhost:10000");

        // given
        assertThat(tested.equals(null), is(false));
        assertThat(tested.equals(""), is(false));
        assertThat(tested.equals(tested), is(true));
        assertThat(tested.equals(new TribClusterNode("localhost:10000")), is(true));
        assertThat(tested.equals(new TribClusterNode("localhost:10001")), is(false));
    }
}
