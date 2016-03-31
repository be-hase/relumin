package com.behase.relumin.support;

import com.behase.relumin.exception.ApiException;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.model.param.CreateClusterParam;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.boot.test.OutputCapture;
import redis.clients.jedis.Jedis;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.*;

@Slf4j
public class RedisTribTest {
    @Spy
    private RedisTrib redisTrib = new RedisTrib();

    @Mock
    private TribClusterNode tribClusterNode;

    @Mock
    private Jedis jedis;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getCreateClusterParams_invalid_replica_then_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Replicas must be equal or longer than 0"));

        redisTrib.getCreateClusterParams(-1, null);
    }

    @Test
    public void getCreateClusterParams() {
        doNothing().when(redisTrib).checkCreateParameters();
        doNothing().when(redisTrib).allocSlots();
        doReturn(Lists.newArrayList()).when(redisTrib).getCreateClusterParams(anyInt(), anySet());

        assertThat(redisTrib.getCreateClusterParams(2, null), is(empty()));
    }

    @Test
    public void checkCreateParameters_master_nodes_need_more_than_3() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Redis Cluster requires at least 3 master nodes"));

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(null, null, null));
        Whitebox.setInternalState(redisTrib, "replicas", 1);
        redisTrib.checkCreateParameters();
    }

    @Test
    public void checkCreateParameters() {
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(null, null, null));
        Whitebox.setInternalState(redisTrib, "replicas", 0);
        redisTrib.checkCreateParameters();

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(null, null, null, null));
        Whitebox.setInternalState(redisTrib, "replicas", 0);
        redisTrib.checkCreateParameters();

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(null, null, null, null, null, null));
        Whitebox.setInternalState(redisTrib, "replicas", 1);
        redisTrib.checkCreateParameters();

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(null, null, null, null, null, null, null));
        Whitebox.setInternalState(redisTrib, "replicas", 1);
        redisTrib.checkCreateParameters();
    }

    @Test
    public void allocSlots() {
        TribClusterNode node1 = new TribClusterNode("host1:1001");
        node1.getNodeInfo().setNodeId("nodeId1");
        TribClusterNode node2 = new TribClusterNode("host1:1002");
        node2.getNodeInfo().setNodeId("nodeId2");
        TribClusterNode node3 = new TribClusterNode("host2:1001");
        node3.getNodeInfo().setNodeId("nodeId3");
        TribClusterNode node4 = new TribClusterNode("host2:1002");
        node4.getNodeInfo().setNodeId("nodeId4");
        TribClusterNode node5 = new TribClusterNode("host3:1001");
        node5.getNodeInfo().setNodeId("nodeId5");
        TribClusterNode node6 = new TribClusterNode("host3:1002");
        node6.getNodeInfo().setNodeId("nodeId6");
        TribClusterNode node7 = new TribClusterNode("host4:1001");
        node7.getNodeInfo().setNodeId("nodeId7");

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(
                node1, node2, node3, node4, node5, node6, node7
        ));
        Whitebox.setInternalState(redisTrib, "replicas", 1);

        redisTrib.allocSlots();

        assertThat(node1.getTmpSlots().size(), is(5461));
        assertThat(node2.getNodeInfo().getMasterNodeId(), is("nodeId3"));
        assertThat(node3.getTmpSlots().size(), is(5461));
        assertThat(node4.getNodeInfo().getMasterNodeId(), is("nodeId5"));
        assertThat(node5.getTmpSlots().size(), is(5462));
        assertThat(node6.getNodeInfo().getMasterNodeId(), is("nodeId1"));
        assertThat(node7.getNodeInfo().getMasterNodeId(), is("nodeId1"));
    }

    @Test
    public void buildCreateClusterParam() {
        TribClusterNode node1 = new TribClusterNode("host1:1001");
        node1.getNodeInfo().setNodeId("nodeId1");
        TribClusterNode node2 = new TribClusterNode("host1:1002");
        node2.getNodeInfo().setNodeId("nodeId2");
        TribClusterNode node3 = new TribClusterNode("host2:1001");
        node3.getNodeInfo().setNodeId("nodeId3");
        TribClusterNode node4 = new TribClusterNode("host2:1002");
        node4.getNodeInfo().setNodeId("nodeId4");
        TribClusterNode node5 = new TribClusterNode("host3:1001");
        node5.getNodeInfo().setNodeId("nodeId5");
        TribClusterNode node6 = new TribClusterNode("host3:1002");
        node6.getNodeInfo().setNodeId("nodeId6");
        TribClusterNode node7 = new TribClusterNode("host4:1001");
        node7.getNodeInfo().setNodeId("nodeId7");

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(
                node1, node2, node3, node4, node5, node6, node7
        ));
        Whitebox.setInternalState(redisTrib, "replicas", 1);

        redisTrib.allocSlots();

        List<CreateClusterParam> result = redisTrib.buildCreateClusterParam();
        log.info("result={}", result);
        assertThat(result.get(0).getStartSlotNumber(), is("0"));
        assertThat(result.get(0).getEndSlotNumber(), is("5460"));
        assertThat(result.get(0).getMaster(), is("host1:1001"));
        assertThat(result.get(0).getMasterNodeId(), is("nodeId1"));
        assertThat(result.get(0).getReplicas(), contains("host3:1002", "host4:1001"));
        assertThat(result.get(1).getStartSlotNumber(), is("5461"));
        assertThat(result.get(1).getEndSlotNumber(), is("10921"));
        assertThat(result.get(1).getMaster(), is("host2:1001"));
        assertThat(result.get(1).getMasterNodeId(), is("nodeId3"));
        assertThat(result.get(1).getReplicas(), hasItem("host1:1002"));
        assertThat(result.get(2).getStartSlotNumber(), is("10922"));
        assertThat(result.get(2).getEndSlotNumber(), is("16383"));
        assertThat(result.get(2).getMaster(), is("host3:1001"));
        assertThat(result.get(2).getMasterNodeId(), is("nodeId5"));
        assertThat(result.get(2).getReplicas(), hasItem("host2:1002"));
    }

    @Test
    public void createCluster() throws Exception {
        List<CreateClusterParam> params = Lists.newArrayList(
                CreateClusterParam.builder()
                        .startSlotNumber("0")
                        .endSlotNumber("5460")
                        .master("host1:1001")
                        .masterNodeId("nodeId1")
                        .replicas(Lists.newArrayList("host3:1002"))
                        .build(),
                CreateClusterParam.builder()
                        .startSlotNumber("5461")
                        .endSlotNumber("10921")
                        .master("host2:1001")
                        .masterNodeId("nodeId3")
                        .replicas(Lists.newArrayList("host1:1002"))
                        .build(),
                CreateClusterParam.builder()
                        .startSlotNumber("10922")
                        .endSlotNumber("16383")
                        .master("host3:1001")
                        .masterNodeId("nodeId5")
                        .replicas(Lists.newArrayList("host2:1002"))
                        .build()
        );

        doReturn(tribClusterNode).when(redisTrib).createTribClusterNode(anyString());
        doReturn(new ClusterNode()).when(tribClusterNode).getNodeInfo();
        doNothing().when(redisTrib).flushNodesConfig();
        doNothing().when(redisTrib).assignConfigEpoch();
        doNothing().when(redisTrib).joinCluster();
        doNothing().when(redisTrib).waitClusterJoin();
        doReturn(Lists.newArrayList()).when(redisTrib).checkCluster();

        redisTrib.createCluster(params);
    }

    @Test
    public void checkCluster_with_host_and_port() {
        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(Lists.newArrayList()).when(redisTrib).checkCluster();

        List<String> result = redisTrib.checkCluster("localhost:10080");
        assertThat(result, is(empty()));
    }

    @Test
    public void loadClusterInfoFromNode() {
        doReturn(tribClusterNode).when(redisTrib).createTribClusterNode("localhost:10000");

        ClusterNode node1 = mock(ClusterNode.class);
        doReturn(false).when(node1).hasFlag(anyString());
        doReturn("localhost:10001").when(node1).getHostAndPort();
        TribClusterNode tribClusterNode1 = mock(TribClusterNode.class);
        doReturn(jedis).when(tribClusterNode1).getJedis();

        ClusterNode node2 = mock(ClusterNode.class);
        doReturn(true).when(node2).hasFlag(anyString());

        ClusterNode node3 = mock(ClusterNode.class);
        doReturn(false).when(node3).hasFlag(anyString());
        doReturn("localhost:10003").when(node3).getHostAndPort();
        TribClusterNode tribClusterNode3 = mock(TribClusterNode.class);
        doReturn(null).when(tribClusterNode3).getJedis();

        doReturn(Lists.newArrayList(node1, node2, node3)).when(tribClusterNode).getFriends();

        doNothing().when(redisTrib).populateNodesReplicasInfo();

        redisTrib.loadClusterInfoFromNode("localhost:10000");

        assertThat(redisTrib.getNodes().size(), is(1));
    }

    @Test
    public void populateNodesReplicasInfo() {
        // ここから
    }

    @Test
    public void flushNodesConfig() {
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));

        redisTrib.flushNodesConfig();

        verify(tribClusterNode, times(2)).flushNodeConfig();
    }

    @Test
    public void assignConfigEpoch() {
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));

        redisTrib.assignConfigEpoch();
    }

    @Test
    public void joinCluster() {
        ClusterNode clusterNode = mock(ClusterNode.class);

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));
        doReturn(clusterNode).when(tribClusterNode).getNodeInfo();
        doReturn(jedis).when(tribClusterNode).getJedis();
        doReturn("localhost").when(clusterNode).getHost();
        doReturn(1000).when(clusterNode).getPort();

        redisTrib.joinCluster();

        verify(tribClusterNode, times(1)).getJedis();
    }

    @Test
    public void waitClusterJoin() throws Exception {
        doReturn(false).doReturn(true).when(redisTrib).isConfigConsistent();
        redisTrib.waitClusterJoin();
        verify(redisTrib, times(2)).isConfigConsistent();
    }

    @Test
    public void checkCluster() {
        doNothing().when(redisTrib).checkConfigConsistency();
        doNothing().when(redisTrib).checkOpenSlots();

        List<String> result = redisTrib.checkCluster();
        assertThat(result, is(empty()));
    }

    @Test
    public void checkConfigConsistency_error() {
        doReturn(false).when(redisTrib).isConfigConsistent();

        redisTrib.checkConfigConsistency();

        assertThat(redisTrib.getErrors().size(), is(1));
    }

    @Test
    public void checkConfigConsistency() throws Exception {
        doReturn(true).when(redisTrib).isConfigConsistent();

        redisTrib.checkConfigConsistency();

        verify(redisTrib, times(0)).clusterError(anyString());
    }

    @Test
    public void isConfigConsistent_true() {
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));
        doReturn("hoge").when(tribClusterNode).getConfigSignature();

        boolean result = redisTrib.isConfigConsistent();
        assertThat(result, is(true));
    }

    @Test
    public void isConfigConsistent_false() {
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));
        doReturn("hoge").doReturn("bar").when(tribClusterNode).getConfigSignature();

        boolean result = redisTrib.isConfigConsistent();
        assertThat(result, is(false));
    }

    @Test
    public void checkOpenSlots_no_open_slots() {
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode));
        doReturn(new ClusterNode()).when(tribClusterNode).getNodeInfo();

        redisTrib.checkOpenSlots();

        assertThat(redisTrib.getErrors(), is(empty()));
    }

    @Test
    public void checkOpenSlots_open_slots_exists() {
        ClusterNode clusterNode = new ClusterNode();
        clusterNode.setImporting(ImmutableMap.of(1, "nodeId1"));
        clusterNode.setMigrating(ImmutableMap.of(2, "nodeId2"));

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode));
        Whitebox.setInternalState(redisTrib, "fix", true);
        doReturn(clusterNode).when(tribClusterNode).getNodeInfo();
        doNothing().when(redisTrib).fixOpenSlot(anyInt());

        redisTrib.checkOpenSlots();

        assertThat(redisTrib.getErrors().size(), is(1));
        verify(redisTrib, times(1)).fixOpenSlot(anyInt());
    }

    @Test
    public void fixOpenSlot_owener_node_is_null_then_throw() {
        expectedEx.expect(ApiException.class);
        expectedEx.expectMessage(containsString("Can't select a slot owner. Impossible to fix"));

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode));
        doReturn(Lists.newArrayList()).when(redisTrib).getSlotOwners(anyInt());
        doReturn(false).when(tribClusterNode).hasFlag(anyString());
        doReturn(new ClusterNode()).when(tribClusterNode).getNodeInfo();
        doReturn(jedis).when(tribClusterNode).getJedis();
        doReturn(null).when(redisTrib).getNodeWithMostKeysInSlot(anyList(), anyInt());

        redisTrib.fixOpenSlot(0);
    }

    @Test
    public void fixOpenSlot_else_case() {
        expectedEx.expect(ApiException.class);
        expectedEx.expectMessage(containsString("Sorry, can't fix this slot yet (work in progress)"));

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode));
        doReturn(Lists.newArrayList()).when(redisTrib).getSlotOwners(anyInt());
        doReturn(true).when(tribClusterNode).hasFlag(anyString());
        doReturn(tribClusterNode).when(redisTrib).getNodeWithMostKeysInSlot(anyList(), anyInt());
        doReturn(new ClusterNode()).when(tribClusterNode).getNodeInfo();
        doReturn(jedis).when(tribClusterNode).getJedis();

        redisTrib.fixOpenSlot(0);
    }

    @Test
    public void fixOpenSlot_case1() {
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));
        doReturn(Lists.newArrayList(tribClusterNode)).when(redisTrib).getSlotOwners(anyInt());
        doReturn(false).when(tribClusterNode).hasFlag(anyString());
        doReturn(ClusterNode.builder().migrating(ImmutableMap.of(0, "nodeId")).build())
                .doReturn(ClusterNode.builder().migrating(ImmutableMap.of()).build())
                .doReturn(ClusterNode.builder().importing(ImmutableMap.of(0, "nodeId")).build())
                .when(tribClusterNode).getNodeInfo();
        doNothing().when(redisTrib).moveSlot(any(), any(), anyInt(), anySet());

        redisTrib.fixOpenSlot(0);

        verify(redisTrib).moveSlot(any(), any(), anyInt(), anySet());
    }

    @Test
    public void fixOpenSlot_case2() {
        TribClusterNode otherTribClusterNode = mock(TribClusterNode.class);

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode, otherTribClusterNode));
        doReturn(Lists.newArrayList(tribClusterNode)).when(redisTrib).getSlotOwners(anyInt());
        doReturn(true).when(tribClusterNode).hasFlag(anyString());
        doReturn(false).when(otherTribClusterNode).hasFlag(anyString());
        doReturn(ClusterNode.builder().migrating(ImmutableMap.of()).build())
                .doReturn(ClusterNode.builder().importing(ImmutableMap.of(0, "nodeId")).build())
                .when(otherTribClusterNode).getNodeInfo();
        doReturn(new ClusterNode()).when(tribClusterNode).getNodeInfo();
        doNothing().when(redisTrib).moveSlot(any(), any(), anyInt(), anySet());
        doReturn(jedis).when(otherTribClusterNode).getJedis();

        redisTrib.fixOpenSlot(0);

        verify(redisTrib).moveSlot(any(), any(), anyInt(), anySet());
        verify(jedis).clusterSetSlotStable(anyInt());
    }

    @Test
    public void fixOpenSlot_case3() {
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode));
        doReturn(Lists.newArrayList(tribClusterNode)).when(redisTrib).getSlotOwners(anyInt());
        doReturn(false).when(tribClusterNode).hasFlag(anyString());
        doReturn(ClusterNode.builder().migrating(ImmutableMap.of(0, "nodeId")).build())
                .when(tribClusterNode).getNodeInfo();
        doReturn(jedis).when(tribClusterNode).getJedis();
        doReturn(Lists.newArrayList()).when(jedis).clusterGetKeysInSlot(anyInt(), anyInt());

        redisTrib.fixOpenSlot(0);

        verify(jedis).clusterSetSlotStable(anyInt());
    }

    @Test
    public void moveSlot_cold_off() {
        TribClusterNode source = mock(TribClusterNode.class);
        TribClusterNode target = mock(TribClusterNode.class);

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(source, target));
        doReturn(ClusterNode.builder().hostAndPort("localhost:10000").build()).when(source).getNodeInfo();
        doReturn(ClusterNode.builder().hostAndPort("localhost:10001").build()).when(target).getNodeInfo();
        doReturn(jedis).when(source).getJedis();
        doReturn(jedis).when(target).getJedis();
        doReturn(Lists.newArrayList("key1", "key2"))
                .doReturn(Lists.newArrayList())
                .when(jedis).clusterGetKeysInSlot(anyInt(), anyInt());

        redisTrib.moveSlot(source, target, 0, null);

        verify(jedis).clusterSetSlotImporting(anyInt(), anyString());
        verify(jedis).clusterSetSlotMigrating(anyInt(), anyString());
        verify(jedis, times(2)).clusterSetSlotNode(anyInt(), anyString());
    }

    @Test
    public void moveSlot_cold_on_and_update_on() {
        TribClusterNode source = mock(TribClusterNode.class);
        TribClusterNode target = mock(TribClusterNode.class);

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(source, target));
        doReturn(ClusterNode.builder().hostAndPort("localhost:10000").build()).when(source).getNodeInfo();
        doReturn(ClusterNode.builder().hostAndPort("localhost:10001").build()).when(target).getNodeInfo();
        doReturn(jedis).when(source).getJedis();
        doReturn(jedis).when(target).getJedis();
        doReturn(Lists.newArrayList("key1", "key2"))
                .doReturn(Lists.newArrayList())
                .when(jedis).clusterGetKeysInSlot(anyInt(), anyInt());

        redisTrib.moveSlot(source, target, 0, Sets.newHashSet("cold", "update"));
        verify(source).getTmpSlots();
        verify(target).getTmpSlots();
    }

    @Test
    public void moveSlot_migrate_fail_then_throw_exception() {
        expectedEx.expect(ApiException.class);

        TribClusterNode source = mock(TribClusterNode.class);
        TribClusterNode target = mock(TribClusterNode.class);

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(source, target));
        doReturn(ClusterNode.builder().hostAndPort("localhost:10000").build()).when(source).getNodeInfo();
        doReturn(ClusterNode.builder().hostAndPort("localhost:10001").build()).when(target).getNodeInfo();
        doReturn(jedis).when(source).getJedis();
        doReturn(jedis).when(target).getJedis();
        doReturn(Lists.newArrayList("key1", "key2"))
                .doReturn(Lists.newArrayList())
                .when(jedis).clusterGetKeysInSlot(anyInt(), anyInt());
        doThrow(Exception.class).when(jedis).migrate(anyString(), anyInt(), anyString(), anyInt(), anyInt());

        redisTrib.moveSlot(source, target, 0, Sets.newHashSet("cold", "update"));
    }


    /*
    @Value("${test.redis.normalCluster}")
    private String testRedisNormalCluster;

    @Value("${test.redis.emptyCluster}")
    private String testRedisEmptyCluster;

    @Value("${test.redis.emptyClusterAll}")
    private String testRedisEmptyClusterAll;

    @Value("${test.redis.normalStandAlone}")
    private String testRedisNormalStandAlone;

    @Value("${test.redis.normalStandAloneAll}")
    private String testRedisNormalStandAloneAll;

    @Value("${test.redis.emptyStandAlone}")
    private String testRedisEmptyStandAlone;

    @Value("${test.redis.emptyStandAloneAll}")
    private String testRedisEmptyStandAloneAll;

    private RedisTrib redisTrib;

    @Before
    public void before() {
        // normal cluster
        try (JedisCluster jedisCluster = JedisUtils.getJedisClusterByHostAndPort(testRedisNormalCluster)) {
            jedisCluster.set("hoge", "hoge");
        } catch (Exception e) {
        }

        // empty cluster (and reset)
        for (String item : StringUtils.split(testRedisEmptyClusterAll, ",")) {
            try (Jedis jedis = JedisUtils.getJedisByHostAndPort(item)) {
                try {
                    jedis.flushAll();
                } catch (Exception e) {
                }
                try {
                    jedis.clusterReset(Reset.HARD);
                } catch (Exception e) {
                }
            } catch (Exception e) {
            }
        }

        for (String item : StringUtils.split(testRedisEmptyStandAloneAll, ",")) {
            try (Jedis jedis = JedisUtils.getJedisByHostAndPort(item)) {
                jedis.flushAll();
            } catch (Exception e) {
            }
        }
    }

    @After
    public void after() throws Exception {
        if (redisTrib != null) {
            redisTrib.close();
        }
    }

    @Test
    public void test() {
    }

    @Test
    public void buildCreateClusterParam1() throws Exception {
        redisTrib = new RedisTrib();
        Whitebox.setInternalState(redisTrib, "replicas", 1);
        String[] hostAndPorts = {
                "2.2.2.2:7", "2.2.2.2:8", "2.2.2.2:9", "2.2.2.2:10", "2.2.2.2:11", "2.2.2.2:12",
                "2.2.2.2:1", "2.2.2.2:2", "2.2.2.2:3", "2.2.2.2:4", "2.2.2.2:5", "2.2.2.2:6",
                "1.1.1.1:7", "1.1.1.1:8", "1.1.1.1:9", "1.1.1.1:10", "1.1.1.1:11", "1.1.1.1:12",
                "1.1.1.1:1", "1.1.1.1:2", "1.1.1.1:3", "1.1.1.1:4", "1.1.1.1:5", "1.1.1.1:6",
        };
        for (String hostAndPort : hostAndPorts) {
            TribClusterNode node = new TribClusterNode(hostAndPort);
            node.getInfo().setNodeId("nodeId:" + hostAndPort);
            redisTrib.addNodes(node);
        }

        redisTrib.allocSlots();

        List<CreateClusterParam> params = redisTrib.buildCreateClusterParam();
        CreateClusterParam param;

        assertThat(params.size(), is(12));

        param = params.get(0);
        assertThat(param.getStartSlotNumber(), is("0"));
        assertThat(param.getEndSlotNumber(), is("1364"));
        assertThat(param.getMaster(), is("1.1.1.1:1"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("2.2.2.2:7")));
        param = params.get(1);
        assertThat(param.getStartSlotNumber(), is("1365"));
        assertThat(param.getEndSlotNumber(), is("2729"));
        assertThat(param.getMaster(), is("2.2.2.2:1"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:7")));
        param = params.get(10);
        assertThat(param.getStartSlotNumber(), is("13650"));
        assertThat(param.getEndSlotNumber(), is("15014"));
        assertThat(param.getMaster(), is("1.1.1.1:6"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("2.2.2.2:12")));
        param = params.get(11);
        assertThat(param.getStartSlotNumber(), is("15015"));
        assertThat(param.getEndSlotNumber(), is("16383"));
        assertThat(param.getMaster(), is("2.2.2.2:6"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:12")));
    }

    @Test
    public void buildCreateClusterParam2() throws Exception {
        redisTrib = new RedisTrib();
        Whitebox.setInternalState(redisTrib, "replicas", 2);
        String[] hostAndPorts = {
                "1.1.1.1:1", "1.1.1.1:2", "1.1.1.1:3", "1.1.1.1:4", "1.1.1.1:5", "1.1.1.1:6",
                "1.1.1.1:7", "1.1.1.1:8", "1.1.1.1:9", "1.1.1.1:10", "1.1.1.1:11", "1.1.1.1:12",
                "2.2.2.2:1", "2.2.2.2:2", "2.2.2.2:3", "2.2.2.2:4", "2.2.2.2:5", "2.2.2.2:6",
                "2.2.2.2:7", "2.2.2.2:8", "2.2.2.2:9", "2.2.2.2:10", "2.2.2.2:11", "2.2.2.2:12",
                "3.3.3.3:1", "3.3.3.3:2", "3.3.3.3:3", "3.3.3.3:4", "3.3.3.3:5", "3.3.3.3:6",
                "3.3.3.3:7", "3.3.3.3:8", "3.3.3.3:9", "3.3.3.3:10", "3.3.3.3:11", "3.3.3.3:12",
        };
        for (String hostAndPort : hostAndPorts) {
            TribClusterNode node = new TribClusterNode(hostAndPort);
            node.getInfo().setNodeId("nodeId:" + hostAndPort);
            redisTrib.addNodes(node);
        }

        redisTrib.allocSlots();

        List<CreateClusterParam> params = redisTrib.buildCreateClusterParam();
        CreateClusterParam param;

        assertThat(params.size(), is(12));

        param = params.get(0);
        assertThat(param.getStartSlotNumber(), is("0"));
        assertThat(param.getEndSlotNumber(), is("1364"));
        assertThat(param.getMaster(), is("1.1.1.1:1"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("2.2.2.2:5", "3.3.3.3:5")));
        param = params.get(1);
        assertThat(param.getStartSlotNumber(), is("1365"));
        assertThat(param.getEndSlotNumber(), is("2729"));
        assertThat(param.getMaster(), is("2.2.2.2:1"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:5", "3.3.3.3:6")));
        param = params.get(10);
        assertThat(param.getStartSlotNumber(), is("13650"));
        assertThat(param.getEndSlotNumber(), is("15014"));
        assertThat(param.getMaster(), is("2.2.2.2:4"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:11", "3.3.3.3:12")));
        param = params.get(11);
        assertThat(param.getStartSlotNumber(), is("15015"));
        assertThat(param.getEndSlotNumber(), is("16383"));
        assertThat(param.getMaster(), is("3.3.3.3:4"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:12", "2.2.2.2:12")));
    }

    @Test
    public void buildCreateClusterParam3() throws Exception {
        redisTrib = new RedisTrib();
        Whitebox.setInternalState(redisTrib, "replicas", 1);
        String[] hostAndPorts = {
                "1.1.1.1:1", "1.1.1.1:2", "1.1.1.1:3", "1.1.1.1:4", "1.1.1.1:5", "1.1.1.1:6",
                "2.2.2.2:1", "2.2.2.2:2", "2.2.2.2:3", "2.2.2.2:4", "2.2.2.2:5", "2.2.2.2:6",
                "2.2.2.2:7", "2.2.2.2:8", "2.2.2.2:9", "2.2.2.2:10", "2.2.2.2:11", "2.2.2.2:12",
                "3.3.3.3:1", "3.3.3.3:2", "3.3.3.3:3", "3.3.3.3:4", "3.3.3.3:5", "3.3.3.3:6",
                "3.3.3.3:7", "3.3.3.3:8", "3.3.3.3:9", "3.3.3.3:10", "3.3.3.3:11", "3.3.3.3:12",
        };
        for (String hostAndPort : hostAndPorts) {
            TribClusterNode node = new TribClusterNode(hostAndPort);
            node.getInfo().setNodeId("nodeId:" + hostAndPort);
            redisTrib.addNodes(node);
        }

        redisTrib.allocSlots();

        List<CreateClusterParam> params = redisTrib.buildCreateClusterParam();
        CreateClusterParam param;

        assertThat(params.size(), is(15));
        param = params.get(0);
        assertThat(param.getStartSlotNumber(), is("0"));
        assertThat(param.getEndSlotNumber(), is("1091"));
        assertThat(param.getMaster(), is("1.1.1.1:1"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("2.2.2.2:6")));
        param = params.get(1);
        assertThat(param.getStartSlotNumber(), is("1092"));
        assertThat(param.getEndSlotNumber(), is("2183"));
        assertThat(param.getMaster(), is("2.2.2.2:1"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:6")));
        param = params.get(13);
        assertThat(param.getStartSlotNumber(), is("14196"));
        assertThat(param.getEndSlotNumber(), is("15287"));
        assertThat(param.getMaster(), is("2.2.2.2:5"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("3.3.3.3:12")));
        param = params.get(14);
        assertThat(param.getStartSlotNumber(), is("15288"));
        assertThat(param.getEndSlotNumber(), is("16383"));
        assertThat(param.getMaster(), is("3.3.3.3:5"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("2.2.2.2:12")));
    }

    @Test
    public void buildCreateClusterParam4() throws Exception {
        redisTrib = new RedisTrib();
        Whitebox.setInternalState(redisTrib, "replicas", 7);
        String[] hostAndPorts = {
                "1.1.1.1:1", "1.1.1.1:2", "1.1.1.1:3", "1.1.1.1:4", "1.1.1.1:5", "1.1.1.1:6",
                "1.1.1.1:7", "1.1.1.1:8", "1.1.1.1:9", "1.1.1.1:10", "1.1.1.1:11", "1.1.1.1:12",
                "2.2.2.2:1", "2.2.2.2:2", "2.2.2.2:3", "2.2.2.2:4", "2.2.2.2:5", "2.2.2.2:6",
                "2.2.2.2:7", "2.2.2.2:8", "2.2.2.2:9", "2.2.2.2:10", "2.2.2.2:11", "2.2.2.2:12",
                "3.3.3.3:1", "3.3.3.3:2", "3.3.3.3:3", "3.3.3.3:4", "3.3.3.3:5", "3.3.3.3:6",
                "3.3.3.3:7", "3.3.3.3:8", "3.3.3.3:9", "3.3.3.3:10", "3.3.3.3:11", "3.3.3.3:12",
        };
        for (String hostAndPort : hostAndPorts) {
            TribClusterNode node = new TribClusterNode(hostAndPort);
            node.getInfo().setNodeId("nodeId:" + hostAndPort);
            redisTrib.addNodes(node);
        }

        redisTrib.allocSlots();

        List<CreateClusterParam> params = redisTrib.buildCreateClusterParam();
        CreateClusterParam param;

        assertThat(params.size(), is(4));
        param = params.get(0);
        assertThat(param.getStartSlotNumber(), is("0"));
        assertThat(param.getEndSlotNumber(), is("4095"));
        assertThat(param.getMaster(), is("1.1.1.1:1"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:3", "1.1.1.1:4", "2.2.2.2:2", "2.2.2.2:3", "2.2.2.2:4", "3.3.3.3:2", "3.3.3.3:3", "3.3.3.3:11")));
        param = params.get(1);
        assertThat(param.getStartSlotNumber(), is("4096"));
        assertThat(param.getEndSlotNumber(), is("8191"));
        assertThat(param.getMaster(), is("2.2.2.2:1"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:5", "1.1.1.1:6", "1.1.1.1:12", "2.2.2.2:5", "2.2.2.2:6", "3.3.3.3:4", "3.3.3.3:5", "3.3.3.3:6")));
        param = params.get(2);
        assertThat(param.getStartSlotNumber(), is("8192"));
        assertThat(param.getEndSlotNumber(), is("12287"));
        assertThat(param.getMaster(), is("3.3.3.3:1"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:7", "1.1.1.1:8", "1.1.1.1:9", "2.2.2.2:7", "2.2.2.2:8", "2.2.2.2:12", "3.3.3.3:7", "3.3.3.3:8")));
        param = params.get(3);
        assertThat(param.getStartSlotNumber(), is("12288"));
        assertThat(param.getEndSlotNumber(), is("16383"));
        assertThat(param.getMaster(), is("1.1.1.1:2"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:10", "1.1.1.1:11", "2.2.2.2:9", "2.2.2.2:10", "2.2.2.2:11", "3.3.3.3:9", "3.3.3.3:10", "3.3.3.3:12")));
    }

    @Test
    public void getCreateClusterParam() throws Exception {
        redisTrib = new RedisTrib();
        List<CreateClusterParam> result = redisTrib.getCreateClusterParams(1, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisEmptyClusterAll, ","))));
        log.debug("getRecommendCreateClusterParam result = {}", result);
        List<CreateClusterParam> params = redisTrib.buildCreateClusterParam();

        CreateClusterParam param;
        param = params.get(0);
        assertThat(param.getStartSlotNumber(), is("0"));
        assertThat(param.getEndSlotNumber(), is("5460"));
        assertThat(param.getMaster(), is("192.168.33.11:8000"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("192.168.33.11:8003")));
        param = params.get(1);
        assertThat(param.getStartSlotNumber(), is("5461"));
        assertThat(param.getEndSlotNumber(), is("10921"));
        assertThat(param.getMaster(), is("192.168.33.11:8001"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("192.168.33.11:8004")));
        param = params.get(2);
        assertThat(param.getStartSlotNumber(), is("10922"));
        assertThat(param.getEndSlotNumber(), is("16383"));
        assertThat(param.getMaster(), is("192.168.33.11:8002"));
        assertThat(param.getReplicas(), is(Lists.newArrayList("192.168.33.11:8005")));
    }

    @Test(expected = InvalidParameterException.class)
    public void getCreateClusterParam_redis_is_not_cluster() throws Exception {
        redisTrib = new RedisTrib();
        redisTrib.getCreateClusterParams(0, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisEmptyStandAloneAll, ","))));
    }

    @Test(expected = InvalidParameterException.class)
    public void getCreateClusterParam_redis_is_not_empty() throws Exception {
        redisTrib = new RedisTrib();
        redisTrib.getCreateClusterParams(0, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisNormalCluster, ","))));
    }

    @Test(expected = InvalidParameterException.class)
    public void getCreateClusterParam_not_enough_master() throws Exception {
        redisTrib = new RedisTrib();
        redisTrib.getCreateClusterParams(2, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisEmptyClusterAll, ","))));
    }

    @Test
    public void createCluster() throws Exception {
        redisTrib = new RedisTrib();

        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("5000");
        param1.setMaster("192.168.33.11:8000");
        param1.setReplicas(Lists.newArrayList("192.168.33.11:8003"));
        CreateClusterParam param2 = new CreateClusterParam();
        param2.setStartSlotNumber("5001");
        param2.setEndSlotNumber("10000");
        param2.setMaster("192.168.33.11:8001");
        param2.setReplicas(Lists.newArrayList("192.168.33.11:8004"));
        CreateClusterParam param3 = new CreateClusterParam();
        param3.setStartSlotNumber("10001");
        param3.setEndSlotNumber("16383");
        param3.setMaster("192.168.33.11:8002");
        param3.setReplicas(Lists.newArrayList("192.168.33.11:8005"));
        List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);

        redisTrib.createCluster(params);
    }

    @Test
    public void loadClusterInfoFromNode() throws Exception {
        createCluster();

        redisTrib = new RedisTrib();
        redisTrib.loadClusterInfoFromNode(testRedisEmptyCluster);

        List<TribClusterNode> nodes = redisTrib.getNodes();
        nodes.sort((o1, o2) -> {
            return o1.getInfo().getHostAndPort().compareTo(o2.getInfo().getHostAndPort());
        });

        TribClusterNode node;
        node = nodes.get(0);
        assertThat(node.getInfo().getHostAndPort(), is("192.168.33.11:8000"));
        assertThat(node.getInfo().getServedSlots(), is("0-5000"));
        node = nodes.get(1);
        assertThat(node.getInfo().getHostAndPort(), is("192.168.33.11:8001"));
        assertThat(node.getInfo().getServedSlots(), is("5001-10000"));
        node = nodes.get(4);
        assertThat(node.getInfo().getHostAndPort(), is("192.168.33.11:8004"));
        assertThat(node.getInfo().getServedSlots(), is(""));
        assertThat(node.getInfo().getMasterNodeId(), is(nodes.get(1).getInfo().getNodeId()));
        node = nodes.get(5);
        assertThat(node.getInfo().getHostAndPort(), is("192.168.33.11:8005"));
        assertThat(node.getInfo().getServedSlots(), is(""));
        assertThat(node.getInfo().getMasterNodeId(), is(nodes.get(2).getInfo().getNodeId()));
    }

    @Test
    public void reshardCluster() throws Exception {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("5000");
        param1.setMaster("192.168.33.11:8000");
        param1.setReplicas(Lists.newArrayList("192.168.33.11:8003"));
        CreateClusterParam param2 = new CreateClusterParam();
        param2.setStartSlotNumber("5001");
        param2.setEndSlotNumber("10000");
        param2.setMaster("192.168.33.11:8001");
        param2.setReplicas(Lists.newArrayList("192.168.33.11:8004"));
        CreateClusterParam param3 = new CreateClusterParam();
        param3.setStartSlotNumber("10001");
        param3.setEndSlotNumber("16383");
        param3.setMaster("192.168.33.11:8002");
        param3.setReplicas(Lists.newArrayList("192.168.33.11:8005"));
        List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);

        RedisTrib createTrib = new RedisTrib();
        createTrib.createCluster(params);

        redisTrib = new RedisTrib();
        redisTrib.reshardCluster("192.168.33.11:8000", 2000, "ALL", createTrib.getNodeByHostAndPort("192.168.33.11:8000").getInfo().getNodeId());

        createTrib.close();
    }

    @Test
    public void addNode() throws Exception {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("5000");
        param1.setMaster("192.168.33.11:8000");
        CreateClusterParam param2 = new CreateClusterParam();
        param2.setStartSlotNumber("5001");
        param2.setEndSlotNumber("10000");
        param2.setMaster("192.168.33.11:8001");
        CreateClusterParam param3 = new CreateClusterParam();
        param3.setStartSlotNumber("10001");
        param3.setEndSlotNumber("16383");
        param3.setMaster("192.168.33.11:8002");
        List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);

        RedisTrib createTrib = new RedisTrib();
        createTrib.createCluster(params);

        redisTrib = new RedisTrib();
        redisTrib.addNodeIntoCluster("192.168.33.11:8000", "192.168.33.11:8003");

        createTrib.close();
    }

    @Test
    public void addNodeAsReplicaRandomMaster() throws Exception {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("5000");
        param1.setMaster("192.168.33.11:8000");
        CreateClusterParam param2 = new CreateClusterParam();
        param2.setStartSlotNumber("5001");
        param2.setEndSlotNumber("10000");
        param2.setMaster("192.168.33.11:8001");
        CreateClusterParam param3 = new CreateClusterParam();
        param3.setStartSlotNumber("10001");
        param3.setEndSlotNumber("16383");
        param3.setMaster("192.168.33.11:8002");
        List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);

        RedisTrib createTrib = new RedisTrib();
        createTrib.createCluster(params);

        redisTrib = new RedisTrib();
        redisTrib.addNodeIntoClusterAsReplica("192.168.33.11:8000", "192.168.33.11:8003", null);

        createTrib.close();
    }

    @Test
    public void addNodeAsReplicaSpecifyMaster() throws Exception {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("5000");
        param1.setMaster("192.168.33.11:8000");
        CreateClusterParam param2 = new CreateClusterParam();
        param2.setStartSlotNumber("5001");
        param2.setEndSlotNumber("10000");
        param2.setMaster("192.168.33.11:8001");
        CreateClusterParam param3 = new CreateClusterParam();
        param3.setStartSlotNumber("10001");
        param3.setEndSlotNumber("16383");
        param3.setMaster("192.168.33.11:8002");
        List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);

        RedisTrib createTrib = new RedisTrib();
        createTrib.createCluster(params);

        redisTrib = new RedisTrib();
        redisTrib.addNodeIntoClusterAsReplica("192.168.33.11:8000", "192.168.33.11:8003", createTrib.getNodeByHostAndPort("192.168.33.11:8000").getInfo().getNodeId());

        createTrib.close();
    }

    @Test
    public void deleteNode() throws Exception {
        CreateClusterParam param1 = new CreateClusterParam();
        param1.setStartSlotNumber("0");
        param1.setEndSlotNumber("5000");
        param1.setMaster("192.168.33.11:8000");
        CreateClusterParam param2 = new CreateClusterParam();
        param2.setStartSlotNumber("5001");
        param2.setEndSlotNumber("10000");
        param2.setMaster("192.168.33.11:8001");
        CreateClusterParam param3 = new CreateClusterParam();
        param3.setStartSlotNumber("10001");
        param3.setEndSlotNumber("16383");
        param3.setMaster("192.168.33.11:8002");
        List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);

        RedisTrib createTrib = new RedisTrib();
        createTrib.createCluster(params);

        RedisTrib addRedisTrib = new RedisTrib();
        addRedisTrib.addNodeIntoCluster("192.168.33.11:8000", "192.168.33.11:8003");
        addRedisTrib.waitClusterJoin();

        redisTrib = new RedisTrib();
        redisTrib.deleteNodeOfCluster("192.168.33.11:8000", addRedisTrib.getNodeByHostAndPort("192.168.33.11:8003").getInfo().getNodeId(), null, false);

        addRedisTrib.close();
        createTrib.close();
    }
    */
}
