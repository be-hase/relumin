package com.behase.relumin.support;

import com.behase.relumin.exception.ApiException;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.Cluster;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.OutputCapture;
import redis.clients.jedis.Jedis;

import java.util.LinkedHashSet;
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
@RunWith(MockitoJUnitRunner.class)
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
    }

    @Test
    public void getCreateClusterParams_invalid_replica_then_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Replicas must be equal or longer than 0"));

        redisTrib.getCreateClusterParams(-1, Sets.newHashSet());
    }

    @Test
    public void getCreateClusterParams() {
        doNothing().when(redisTrib).checkCreateParameters();
        doNothing().when(redisTrib).allocSlots();
        doReturn(Lists.newArrayList()).when(redisTrib).getCreateClusterParams(anyInt(), anySet());

        assertThat(redisTrib.getCreateClusterParams(2, Sets.newHashSet()), is(empty()));
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
        doNothing().when(redisTrib).validateClusterAndEmptyNode(any());
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

        doReturn(tribClusterNode).when(redisTrib).createTribClusterNode("localhost:10000");
        doReturn(tribClusterNode1).when(redisTrib).createTribClusterNode("localhost:10001");
        doReturn(tribClusterNode3).when(redisTrib).createTribClusterNode("localhost:10003");
        doReturn(Lists.newArrayList(node1, node2, node3)).when(tribClusterNode).getFriends();
        doNothing().when(redisTrib).populateNodesReplicasInfo();

        redisTrib.loadClusterInfoFromNode("localhost:10000");

        assertThat(redisTrib.getNodes().size(), is(2));
    }

    @Test
    public void populateNodesReplicasInfo() {
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));

        ClusterNode clusterNode = mock(ClusterNode.class);
        doReturn(clusterNode).when(tribClusterNode).getNodeInfo();
        doReturn("masterNodeId1").doReturn("masterNodeId2").when(clusterNode).getMasterNodeId();
        doReturn(tribClusterNode).when(redisTrib).getNodeByNodeId("masterNodeId1");
        doReturn(null).when(redisTrib).getNodeByNodeId("masterNodeId2");

        redisTrib.populateNodesReplicasInfo();

        verify(tribClusterNode).getReplicas();
    }

    @Test
    public void fixCluster() throws Exception {
        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();

        redisTrib.fixCluster("localhost:10080");

        assertThat(Whitebox.getInternalState(redisTrib, "fix"), is(true));
        verify(redisTrib).loadClusterInfoFromNode(anyString());
        verify(redisTrib).checkCluster();
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

        assertThat(redisTrib.getErrors().size(), is(2));
        verify(redisTrib, times(2)).fixOpenSlot(anyInt());
    }

    @Test
    public void getNodeWithMostKeysInSlot() {
        TribClusterNode mock1 = mock(TribClusterNode.class);
        TribClusterNode mock2 = mock(TribClusterNode.class);
        TribClusterNode mock3 = mock(TribClusterNode.class);

        doReturn(jedis).when(mock2).getJedis();
        doReturn(jedis).when(mock3).getJedis();

        doReturn(true).when(mock1).hasFlag("slave");
        doReturn(jedis).when(mock2).getJedis();
        doReturn(jedis).when(mock3).getJedis();
        doReturn(1l).doReturn(2l).when(jedis).clusterCountKeysInSlot(anyInt());

        TribClusterNode result = redisTrib.getNodeWithMostKeysInSlot(Lists.newArrayList(mock1, mock2, mock3), 0);

        assertThat(result, is(mock3));
    }

    @Test
    public void fixOpenSlot_owner_node_is_null_then_throw() {
        expectedEx.expect(ApiException.class);
        expectedEx.expectMessage(containsString("Fix me, some work to do here"));

        doReturn(null).when(redisTrib).getSlotOwner(anyInt());

        redisTrib.fixOpenSlot(0);
    }

    @Test
    public void fixOpenSlot_else_case() {
        expectedEx.expect(ApiException.class);
        expectedEx.expectMessage(containsString("Sorry, can't fix this slot yet (work in progress)"));

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode));
        doReturn(tribClusterNode).when(redisTrib).getSlotOwner(anyInt());
        doReturn(true).when(tribClusterNode).hasFlag(anyString());

        redisTrib.fixOpenSlot(0);
    }

    @Test
    public void fixOpenSlot_case1() {
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));
        doReturn(tribClusterNode).when(redisTrib).getSlotOwner(anyInt());
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
        doReturn(tribClusterNode).when(redisTrib).getSlotOwner(anyInt());
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
        doReturn(tribClusterNode).when(redisTrib).getSlotOwner(anyInt());
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

    @Test(expected = InvalidParameterException.class)
    public void reshardCluster_invalid_slotCount() throws Exception {
        redisTrib.reshardCluster("localhost:10080", -1, "ALL", "toNodeId");
    }

    @Test(expected = InvalidParameterException.class)
    public void reshardCluster_toNodeId_is_blank() throws Exception {
        redisTrib.reshardCluster("localhost:10080", 10, "ALL", "");
    }

    @Test(expected = InvalidParameterException.class)
    public void reshardCluster_fromNodeId_is_blank() throws Exception {
        redisTrib.reshardCluster("localhost:10080", 10, "", "toNodeId");
    }

    @Test
    public void reshardCluster_cluster_has_error() throws Exception {
        expectedEx.expect(ApiException.class);
        expectedEx.expectMessage(containsString("Please fix your cluster problems before resharding"));

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        Whitebox.setInternalState(redisTrib, "errors", Lists.newArrayList("error"));

        redisTrib.reshardCluster("localhost:10080", 10, "ALL", "toNodeId");
    }

    @Test
    public void reshardCluster_ALL() throws Exception {
        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId1").build()).when(node1).getNodeInfo();
        doReturn(true).when(node1).hasFlag("slave");

        TribClusterNode node2 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId2").build()).when(node2).getNodeInfo();
        doReturn(false).when(node2).hasFlag("slave");

        TribClusterNode node3 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId3").build()).when(node3).getNodeInfo();
        doReturn(false).when(node3).hasFlag("slave");

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(node1, node2, node3));
        doReturn(Lists.newArrayList()).when(redisTrib).computeReshardTable(anyList(), anyInt());

        redisTrib.reshardCluster("localhost:10080", 10, "ALL", "nodeId2");

        verify(redisTrib).computeReshardTable(Lists.newArrayList(node3), 10);
    }

    @Test
    public void reshardCluster_specify_fromNodeIds_contains_slave() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("The specified node(nodeId1) is not known or is not a master, please retry"));

        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId1").build()).when(node1).getNodeInfo();
        doReturn(true).when(node1).hasFlag("slave");

        TribClusterNode node2 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId2").build()).when(node2).getNodeInfo();
        doReturn(false).when(node2).hasFlag("slave");

        TribClusterNode node3 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId3").build()).when(node3).getNodeInfo();
        doReturn(false).when(node3).hasFlag("slave");

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(node1, node2, node3));
        doReturn(Lists.newArrayList()).when(redisTrib).computeReshardTable(anyList(), anyInt());

        redisTrib.reshardCluster("localhost:10080", 10, "nodeId1,nodeId3", "nodeId2");

        verify(redisTrib).computeReshardTable(Lists.newArrayList(node3), 10);
    }

    @Test
    public void reshardCluster_specify_fromNodeIds_contains_toNodeId() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Target node is also listed among the source nodes"));

        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId1").build()).when(node1).getNodeInfo();
        doReturn(false).when(node1).hasFlag("slave");

        TribClusterNode node2 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId2").build()).when(node2).getNodeInfo();
        doReturn(false).when(node2).hasFlag("slave");

        TribClusterNode node3 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId3").build()).when(node3).getNodeInfo();
        doReturn(false).when(node3).hasFlag("slave");

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(node1, node2, node3));
        doReturn(Lists.newArrayList()).when(redisTrib).computeReshardTable(anyList(), anyInt());

        redisTrib.reshardCluster("localhost:10080", 10, "nodeId1,nodeId2,nodeId3", "nodeId2");

        verify(redisTrib).computeReshardTable(Lists.newArrayList(node3), 10);
    }

    @Test
    public void reshardCluster_specify_fromNodeIds() throws Exception {
        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId1").build()).when(node1).getNodeInfo();
        doReturn(false).when(node1).hasFlag("slave");

        TribClusterNode node2 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId2").build()).when(node2).getNodeInfo();
        doReturn(false).when(node2).hasFlag("slave");

        TribClusterNode node3 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId3").build()).when(node3).getNodeInfo();
        doReturn(false).when(node3).hasFlag("slave");

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(node1, node2, node3));
        doReturn(Lists.newArrayList()).when(redisTrib).computeReshardTable(anyList(), anyInt());

        redisTrib.reshardCluster("localhost:10080", 10, "nodeId1,nodeId3", "nodeId2");

        verify(redisTrib).computeReshardTable(Lists.newArrayList(node1, node3), 10);
    }

    @Test
    public void computeReshardTable() {
        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder()
                .nodeId("nodeId1")
                .servedSlotsSet(Sets.newTreeSet(Sets.newHashSet(0, 1, 2)))
                .build())
                .when(node1).getNodeInfo();

        TribClusterNode node2 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder()
                .nodeId("nodeId2")
                .servedSlotsSet(Sets.newTreeSet(Sets.newHashSet(3, 4, 5)))
                .build())
                .when(node2).getNodeInfo();

        TribClusterNode node3 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder()
                .nodeId("nodeId3")
                .servedSlotsSet(Sets.newTreeSet(Sets.newHashSet(6, 7, 8)))
                .build())
                .when(node3).getNodeInfo();

        List<RedisTrib.ReshardTable> result = redisTrib.computeReshardTable(Lists.newArrayList(node1, node2, node3), 4);
        assertThat(result.get(0), is(new RedisTrib.ReshardTable(node1, 0)));
        assertThat(result.get(1), is(new RedisTrib.ReshardTable(node1, 1)));
        assertThat(result.get(2), is(new RedisTrib.ReshardTable(node2, 3)));
        assertThat(result.get(3), is(new RedisTrib.ReshardTable(node3, 6)));
    }

    @Test
    public void computeReshardTable_not_enough() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Total slot count which is sum of from nodes is not enough"));

        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder()
                .nodeId("nodeId1")
                .servedSlotsSet(Sets.newTreeSet(Sets.newHashSet(0, 1, 2)))
                .build())
                .when(node1).getNodeInfo();

        List<RedisTrib.ReshardTable> result = redisTrib.computeReshardTable(Lists.newArrayList(node1), 4);
    }

    @Test
    public void reshardClusterBySlots_toNodeId_is_blank() throws Exception {
        expectedEx.expect(InvalidParameterException.class);

        redisTrib.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(0, 1, 2)), "");
    }

    @Test
    public void reshardClusterBySlots_cluster_has_error() throws Exception {
        expectedEx.expect(ApiException.class);
        expectedEx.expectMessage(containsString("Please fix your cluster problems before resharding"));

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        Whitebox.setInternalState(redisTrib, "errors", Lists.newArrayList("error"));

        redisTrib.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(0, 1, 2)), "toNodeId");
    }

    @Test
    public void reshardClusterBySlots_toNode_does_not_exist() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("The specified node is not known or is not a master"));

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        doReturn(null).when(redisTrib).getNodeByNodeId(anyString());

        redisTrib.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(0, 1, 2)), "toNodeId");
    }

    @Test
    public void reshardClusterBySlots_toNode_is_slave() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("The specified node is not known or is not a master"));

        TribClusterNode targetNode = mock(TribClusterNode.class);
        doReturn(true).when(targetNode).hasFlag("slave");

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        doReturn(targetNode).when(redisTrib).getNodeByNodeId(anyString());

        redisTrib.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(0, 1, 2)), "toNodeId");
    }

    @Test
    public void reshardClusterBySlots_notFoundSlot_exists() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Cannot find the nodes which has slots"));

        TribClusterNode targetNode = mock(TribClusterNode.class);
        doReturn(false).when(targetNode).hasFlag("slave");
        doReturn(ClusterNode.builder()
                .nodeId("targetNodeId")
                .servedSlotsSet(Sets.newTreeSet(Sets.newHashSet(0, 1, 2)))
                .build())
                .when(targetNode).getNodeInfo();

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        doReturn(targetNode).when(redisTrib).getNodeByNodeId(anyString());

        redisTrib.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(2, 3, 4)), "toNodeId");
    }

    @Test
    public void reshardClusterBySlots() throws Exception {
        TribClusterNode targetNode = mock(TribClusterNode.class);
        doReturn(false).when(targetNode).hasFlag("slave");
        doReturn(ClusterNode.builder()
                .nodeId("targetNodeId")
                .servedSlotsSet(Sets.newTreeSet(Sets.newHashSet(0, 1, 2)))
                .build())
                .when(targetNode).getNodeInfo();

        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(false).when(node1).hasFlag("slave");
        doReturn(ClusterNode.builder()
                .nodeId("nodeId1")
                .servedSlotsSet(Sets.newTreeSet(Sets.newHashSet(3, 4)))
                .build())
                .when(node1).getNodeInfo();

        TribClusterNode node2 = mock(TribClusterNode.class);
        doReturn(true).when(node2).hasFlag("slave");
        doReturn(ClusterNode.builder()
                .nodeId("nodeId2")
                .servedSlotsSet(Sets.newTreeSet(Sets.newHashSet(5, 6)))
                .build())
                .when(node2).getNodeInfo();

        Whitebox.setInternalState(redisTrib, "nodes", Lists.newArrayList(targetNode, node1, node2));
        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        doReturn(targetNode).when(redisTrib).getNodeByNodeId(anyString());
        doNothing().when(redisTrib).moveSlot(any(), any(), anyInt(), anySet());

        redisTrib.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(2, 3, 4)), "toNodeId");

        verify(redisTrib).moveSlot(node1, targetNode, 3, null);
        verify(redisTrib).moveSlot(node1, targetNode, 4, null);
    }

    @Test
    public void validateNewNode_newNode_is_not_connected() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("error"));

        doThrow(new InvalidParameterException("error")).when(tribClusterNode).connect(true);

        redisTrib.validateClusterAndEmptyNode(tribClusterNode);
    }

    @Test
    public void validateNewNode_newNode_is_not_cluster() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("error"));

        doNothing().when(tribClusterNode).connect(true);
        doThrow(new InvalidParameterException("error")).when(tribClusterNode).assertCluster();

        redisTrib.validateClusterAndEmptyNode(tribClusterNode);
    }

    @Test
    public void validateNewNode_newNode_is_not_empty() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("error"));

        doNothing().when(tribClusterNode).connect(true);
        doNothing().when(tribClusterNode).assertCluster();
        doThrow(new InvalidParameterException("error")).when(tribClusterNode).assertEmpty();

        redisTrib.validateClusterAndEmptyNode(tribClusterNode);
    }

    @Test
    public void addNodeIntoCluster_invalid_newHostAndPort() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Invalid parameter. Node is invalid."));

        redisTrib.addNodeIntoCluster("localhost:10080", "aaaaaaaaaaa");
    }

    @Test
    public void addNodeIntoClusterAsReplica_masterId_does_not_exists() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("No such master ID"));

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        doReturn(null).when(redisTrib).getNodeByNodeId(anyString());

        redisTrib.addNodeIntoClusterAsReplica("localhost:10080", "localhost:10081", "masterNodeId");
    }

    @Test
    public void addNodeIntoClusterAsReplica_masterId_is_RANDOM() throws Exception {
        doReturn(ClusterNode.builder().hostAndPort("localhost:10000").build()).when(tribClusterNode).getNodeInfo();
        doReturn(jedis).when(tribClusterNode).getJedis();

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).checkCluster();
        doReturn(tribClusterNode).when(redisTrib).getMasterWithLeastReplicas();
        doReturn(tribClusterNode).when(redisTrib).createTribClusterNode(anyString());
        doReturn(tribClusterNode).when(redisTrib).getNodeByHostAndPort(anyString());

        redisTrib.addNodeIntoClusterAsReplica("localhost:10080", "localhost:10081", "RANDOM");

        verify(redisTrib).getMasterWithLeastReplicas();
    }

    @Test
    public void deleteNodeOfCluster_nodeId_does_not_exists() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("No such node ID"));

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(null).when(redisTrib).getNodeByNodeId(anyString());

        redisTrib.deleteNodeOfCluster("localhost:10080", "nodeId", null, false);
    }

    @Test
    public void deleteNodeOfCluster_not_empty_node() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("is not empty"));

        doNothing().when(redisTrib).loadClusterInfoFromNode(anyString());
        doReturn(tribClusterNode).when(redisTrib).getNodeByNodeId(anyString());
        doReturn(ClusterNode.builder().servedSlotsSet(Sets.newHashSet(0)).build()).when(tribClusterNode).getNodeInfo();
        doNothing().when(redisTrib).forgotNode(anyString());
        doReturn(jedis).when(tribClusterNode).getJedis();

        redisTrib.deleteNodeOfCluster("localhost:10080", "nodeId", null, false);
    }

}
