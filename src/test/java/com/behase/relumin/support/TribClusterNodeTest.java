package com.behase.relumin.support;

import com.behase.relumin.TestUtils;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.ClusterNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.OutputCapture;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@Slf4j
public class TribClusterNodeTest {
    @Spy
    private TribClusterNode tribClusterNode = new TribClusterNode("localhost:10000");

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
        MockitoAnnotations.initMocks(this);
        doReturn(jedisSupport).when(tribClusterNode).createJedisSupport();
        doReturn(jedis).when(jedisSupport).getJedisByHostAndPort(anyString());
    }

    private void initConnect() {
        doReturn("PONG").when(jedis).ping();
        tribClusterNode.connect();
    }

    @Test
    public void connect_abort_is_true_and_cannot_connect_redis_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Failed to connect to node"));

        doThrow(Exception.class).when(jedis).ping();

        tribClusterNode.connect(true);
    }

    @Test
    public void connect_abort_is_true_and_not_pong_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Failed to connect to node"));

        doReturn("hoge").when(jedis).ping();

        tribClusterNode.connect(true);
    }

    @Test
    public void connect() throws Exception {
        doReturn("PONG").when(jedis).ping();
        tribClusterNode.connect(true);
        assertThat(tribClusterNode.getJedis(), is(not(nullValue())));
    }

    @Test
    public void assertCluster_clusterEnabled_is_blank_then_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("is not configured as a cluster node"));

        initConnect();
        doReturn(Maps.newHashMap()).when(jedisSupport).parseInfoResult(anyString());

        tribClusterNode.assertCluster();
    }

    @Test
    public void assertCluster_clusterEnabled_is_not_1_then_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("is not configured as a cluster node"));

        initConnect();
        doReturn(ImmutableMap.of("cluster_enabled", "0")).when(jedisSupport).parseInfoResult(anyString());

        tribClusterNode.assertCluster();
    }

    @Test
    public void assertEmpty_db0_is_not_null_then_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("is not empty. Either the node already knows other nodes"));

        initConnect();
        doReturn(ImmutableMap.of("db0", "aaa")).when(jedisSupport).parseInfoResult(anyString());
        doReturn(ImmutableMap.of("cluster_known_nodes", "1")).when(jedisSupport).parseClusterInfoResult(anyString());

        tribClusterNode.assertEmpty();
    }

    @Test
    public void assertEmpty_cluster_known_nodes_is_not_1_then_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("is not empty. Either the node already knows other nodes"));

        initConnect();
        doReturn(Maps.newHashMap()).when(jedisSupport).parseInfoResult(anyString());
        doReturn(ImmutableMap.of("cluster_known_nodes", "0")).when(jedisSupport).parseClusterInfoResult(anyString());

        tribClusterNode.assertEmpty();
    }

    @Test
    public void assertEmpty() {
        initConnect();
        doReturn(Maps.newHashMap()).when(jedisSupport).parseInfoResult(anyString());
        doReturn(ImmutableMap.of("cluster_known_nodes", "1")).when(jedisSupport).parseClusterInfoResult(anyString());

        tribClusterNode.assertEmpty();
    }

    @Test
    public void loadInfo_getFriend_true() {
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

        tribClusterNode.loadInfo(true);

        assertThat(tribClusterNode.getInfo().getNodeId(), is("nodeId1"));
        assertThat(tribClusterNode.getFriends().get(0).getNodeId(), is("nodeId2"));
    }

    @Test
    public void loadInfo_getFriend_false() {
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

        tribClusterNode.loadInfo();

        assertThat(tribClusterNode.getInfo().getNodeId(), is("nodeId1"));
        assertThat(tribClusterNode.getFriends(), is(empty()));
    }
}
