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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
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
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.*;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class RedisTribTest {
    @Spy
    private RedisTrib tested = new RedisTrib();

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

        // when
        tested.getCreateClusterParams(-1, Sets.newHashSet());
    }

    @Test
    public void getCreateClusterParams() {
        // given
        doNothing().when(tested).checkCreateParameters();
        doNothing().when(tested).allocSlots();
        doReturn(Lists.newArrayList()).when(tested).getCreateClusterParams(anyInt(), anySet());

        // when
        List<CreateClusterParam> result = tested.getCreateClusterParams(2, Sets.newHashSet());

        // then
        assertThat(result, is(empty()));
    }

    @Test
    public void checkCreateParameters_master_nodes_need_more_than_3() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Redis Cluster requires at least 3 master nodes"));

        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(null, null, null));
        Whitebox.setInternalState(tested, "replicas", 1);

        // when
        tested.checkCreateParameters();
    }

    @Test
    public void checkCreateParameters() {
        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(null, null, null));
        Whitebox.setInternalState(tested, "replicas", 0);
        // when
        tested.checkCreateParameters();

        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(null, null, null, null));
        Whitebox.setInternalState(tested, "replicas", 0);
        // when
        tested.checkCreateParameters();

        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(null, null, null, null, null, null));
        Whitebox.setInternalState(tested, "replicas", 1);
        // when
        tested.checkCreateParameters();

        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(null, null, null, null, null, null, null));
        Whitebox.setInternalState(tested, "replicas", 1);
        // when
        tested.checkCreateParameters();
    }

    @Test
    public void allocSlots() {
        // given
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

        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(
                node1, node2, node3, node4, node5, node6, node7
        ));
        Whitebox.setInternalState(tested, "replicas", 1);

        // when
        tested.allocSlots();

        // then
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
        // given
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

        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(
                node1, node2, node3, node4, node5, node6, node7
        ));
        Whitebox.setInternalState(tested, "replicas", 1);

        // when
        tested.allocSlots();
        List<CreateClusterParam> result = tested.buildCreateClusterParam();

        // then
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
        // given
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

        doReturn(tribClusterNode).when(tested).createTribClusterNode(anyString());
        doReturn(new ClusterNode()).when(tribClusterNode).getNodeInfo();
        doNothing().when(tested).validateClusterAndEmptyNode(any());
        doNothing().when(tested).flushNodesConfig();
        doNothing().when(tested).assignConfigEpoch();
        doNothing().when(tested).joinCluster();
        doNothing().when(tested).waitClusterJoin();
        doReturn(Lists.newArrayList()).when(tested).checkCluster();

        // when, then
        tested.createCluster(params);
    }

    @Test
    public void checkCluster_with_host_and_port() {
        // given
        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(Lists.newArrayList()).when(tested).checkCluster();

        // when
        List<String> result = tested.checkCluster("localhost:10080");

        // then
        assertThat(result, is(empty()));
    }

    @Test
    public void loadClusterInfoFromNode() {
        // given
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

        doReturn(tribClusterNode).when(tested).createTribClusterNode("localhost:10000");
        doReturn(tribClusterNode1).when(tested).createTribClusterNode("localhost:10001");
        doReturn(tribClusterNode3).when(tested).createTribClusterNode("localhost:10003");
        doReturn(Lists.newArrayList(node1, node2, node3)).when(tribClusterNode).getFriends();
        doNothing().when(tested).populateNodesReplicasInfo();

        // when
        tested.loadClusterInfoFromNode("localhost:10000");

        // then
        assertThat(tested.getNodes().size(), is(2));
    }

    @Test
    public void populateNodesReplicasInfo() {
        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));

        ClusterNode clusterNode = mock(ClusterNode.class);
        doReturn(clusterNode).when(tribClusterNode).getNodeInfo();
        doReturn("masterNodeId1").doReturn("masterNodeId2").when(clusterNode).getMasterNodeId();
        doReturn(tribClusterNode).when(tested).getNodeByNodeId("masterNodeId1");
        doReturn(null).when(tested).getNodeByNodeId("masterNodeId2");

        // when
        tested.populateNodesReplicasInfo();

        // then
        verify(tribClusterNode).getReplicas();
    }

    @Test
    public void fixCluster() throws Exception {
        // given
        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();

        // when
        tested.fixCluster("localhost:10080");

        // then
        assertThat(Whitebox.getInternalState(tested, "fix"), is(true));
        verify(tested).loadClusterInfoFromNode(anyString());
        verify(tested).checkCluster();
    }

    @Test
    public void flushNodesConfig() {
        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));

        // when
        tested.flushNodesConfig();

        // then
        verify(tribClusterNode, times(2)).flushNodeConfig();
    }

    @Test
    public void assignConfigEpoch() {
        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));

        // when, then
        tested.assignConfigEpoch();
    }

    @Test
    public void joinCluster() {
        // given
        ClusterNode clusterNode = mock(ClusterNode.class);

        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));
        doReturn(clusterNode).when(tribClusterNode).getNodeInfo();
        doReturn(jedis).when(tribClusterNode).getJedis();
        doReturn("localhost").when(clusterNode).getHost();
        doReturn(1000).when(clusterNode).getPort();

        // when
        tested.joinCluster();

        // then
        verify(tribClusterNode, times(1)).getJedis();
    }

    @Test
    public void waitClusterJoin() throws Exception {
        // given
        doReturn(false).doReturn(true).when(tested).isConfigConsistent();

        // when
        tested.waitClusterJoin();

        // then
        verify(tested, times(2)).isConfigConsistent();
    }

    @Test
    public void checkCluster() {
        // given
        doNothing().when(tested).checkConfigConsistency();
        doNothing().when(tested).checkOpenSlots();

        // when
        List<String> result = tested.checkCluster();

        // then
        assertThat(result, is(empty()));
    }

    @Test
    public void checkConfigConsistency_error() {
        // given
        doReturn(false).when(tested).isConfigConsistent();

        // when
        tested.checkConfigConsistency();

        // then
        assertThat(tested.getErrors().size(), is(1));
    }

    @Test
    public void checkConfigConsistency() throws Exception {
        // given
        doReturn(true).when(tested).isConfigConsistent();

        // when
        tested.checkConfigConsistency();

        // then
        verify(tested, times(0)).clusterError(anyString());
    }

    @Test
    public void isConfigConsistent_true() {
        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));
        doReturn("hoge").when(tribClusterNode).getConfigSignature();

        // when
        boolean result = tested.isConfigConsistent();

        // then
        assertThat(result, is(true));
    }

    @Test
    public void isConfigConsistent_false() {
        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));
        doReturn("hoge").doReturn("bar").when(tribClusterNode).getConfigSignature();

        // when
        boolean result = tested.isConfigConsistent();

        // then
        assertThat(result, is(false));
    }

    @Test
    public void checkOpenSlots_no_open_slots() {
        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode));
        doReturn(new ClusterNode()).when(tribClusterNode).getNodeInfo();

        // when
        tested.checkOpenSlots();

        // then
        assertThat(tested.getErrors(), is(empty()));
    }

    @Test
    public void checkOpenSlots_open_slots_exists() {
        // given
        ClusterNode clusterNode = new ClusterNode();
        clusterNode.setImporting(ImmutableMap.of(1, "nodeId1"));
        clusterNode.setMigrating(ImmutableMap.of(2, "nodeId2"));

        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode));
        Whitebox.setInternalState(tested, "fix", true);
        doReturn(clusterNode).when(tribClusterNode).getNodeInfo();
        doNothing().when(tested).fixOpenSlot(anyInt());

        // when
        tested.checkOpenSlots();

        // then
        assertThat(tested.getErrors().size(), is(2));
        verify(tested, times(2)).fixOpenSlot(anyInt());
    }

    @Test
    public void getNodeWithMostKeysInSlot() {
        // given
        TribClusterNode mock1 = mock(TribClusterNode.class);
        TribClusterNode mock2 = mock(TribClusterNode.class);
        TribClusterNode mock3 = mock(TribClusterNode.class);

        doReturn(jedis).when(mock2).getJedis();
        doReturn(jedis).when(mock3).getJedis();

        doReturn(true).when(mock1).hasFlag("slave");
        doReturn(jedis).when(mock2).getJedis();
        doReturn(jedis).when(mock3).getJedis();
        doReturn(1l).doReturn(2l).when(jedis).clusterCountKeysInSlot(anyInt());

        // when
        TribClusterNode result = tested.getNodeWithMostKeysInSlot(Lists.newArrayList(mock1, mock2, mock3), 0);

        // then
        assertThat(result, is(mock3));
    }

    @Test
    public void fixOpenSlot_owner_node_is_null_then_throw() {
        expectedEx.expect(ApiException.class);
        expectedEx.expectMessage(containsString("Fix me, some work to do here"));

        // given
        doReturn(null).when(tested).getSlotOwner(anyInt());

        // then
        tested.fixOpenSlot(0);
    }

    @Test
    public void fixOpenSlot_else_case() {
        expectedEx.expect(ApiException.class);
        expectedEx.expectMessage(containsString("Sorry, can't fix this slot yet (work in progress)"));

        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode));
        doReturn(tribClusterNode).when(tested).getSlotOwner(anyInt());
        doReturn(true).when(tribClusterNode).hasFlag(anyString());

        // when
        tested.fixOpenSlot(0);
    }

    @Test
    public void fixOpenSlot_case1() {
        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode, tribClusterNode));
        doReturn(tribClusterNode).when(tested).getSlotOwner(anyInt());
        doReturn(false).when(tribClusterNode).hasFlag(anyString());
        doReturn(ClusterNode.builder().migrating(ImmutableMap.of(0, "nodeId")).build())
                .doReturn(ClusterNode.builder().migrating(ImmutableMap.of()).build())
                .doReturn(ClusterNode.builder().importing(ImmutableMap.of(0, "nodeId")).build())
                .when(tribClusterNode).getNodeInfo();
        doNothing().when(tested).moveSlot(any(), any(), anyInt(), anySet());

        // when
        tested.fixOpenSlot(0);

        // then
        verify(tested).moveSlot(any(), any(), anyInt(), anySet());
    }

    @Test
    public void fixOpenSlot_case2() {
        // given
        TribClusterNode otherTribClusterNode = mock(TribClusterNode.class);

        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode, otherTribClusterNode));
        doReturn(tribClusterNode).when(tested).getSlotOwner(anyInt());
        doReturn(true).when(tribClusterNode).hasFlag(anyString());
        doReturn(false).when(otherTribClusterNode).hasFlag(anyString());
        doReturn(ClusterNode.builder().migrating(ImmutableMap.of()).build())
                .doReturn(ClusterNode.builder().importing(ImmutableMap.of(0, "nodeId")).build())
                .when(otherTribClusterNode).getNodeInfo();
        doReturn(new ClusterNode()).when(tribClusterNode).getNodeInfo();
        doNothing().when(tested).moveSlot(any(), any(), anyInt(), anySet());
        doReturn(jedis).when(otherTribClusterNode).getJedis();

        // when
        tested.fixOpenSlot(0);

        // then
        verify(tested).moveSlot(any(), any(), anyInt(), anySet());
        verify(jedis).clusterSetSlotStable(anyInt());
    }

    @Test
    public void fixOpenSlot_case3() {
        // given
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(tribClusterNode));
        doReturn(tribClusterNode).when(tested).getSlotOwner(anyInt());
        doReturn(false).when(tribClusterNode).hasFlag(anyString());
        doReturn(ClusterNode.builder().migrating(ImmutableMap.of(0, "nodeId")).build())
                .when(tribClusterNode).getNodeInfo();
        doReturn(jedis).when(tribClusterNode).getJedis();
        doReturn(Lists.newArrayList()).when(jedis).clusterGetKeysInSlot(anyInt(), anyInt());

        // when
        tested.fixOpenSlot(0);

        // then
        verify(jedis).clusterSetSlotStable(anyInt());
    }

    @Test
    public void moveSlot_cold_off() {
        // given
        TribClusterNode source = mock(TribClusterNode.class);
        TribClusterNode target = mock(TribClusterNode.class);

        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(source, target));
        doReturn(ClusterNode.builder().hostAndPort("localhost:10000").build()).when(source).getNodeInfo();
        doReturn(ClusterNode.builder().hostAndPort("localhost:10001").build()).when(target).getNodeInfo();
        doReturn(jedis).when(source).getJedis();
        doReturn(jedis).when(target).getJedis();
        doReturn(Lists.newArrayList("key1", "key2"))
                .doReturn(Lists.newArrayList())
                .when(jedis).clusterGetKeysInSlot(anyInt(), anyInt());

        // when
        tested.moveSlot(source, target, 0, null);

        // then
        verify(jedis).clusterSetSlotImporting(anyInt(), anyString());
        verify(jedis).clusterSetSlotMigrating(anyInt(), anyString());
        verify(jedis, times(2)).clusterSetSlotNode(anyInt(), anyString());
    }

    @Test
    public void moveSlot_cold_on_and_update_on() {
        // given
        TribClusterNode source = mock(TribClusterNode.class);
        TribClusterNode target = mock(TribClusterNode.class);

        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(source, target));
        doReturn(ClusterNode.builder().hostAndPort("localhost:10000").build()).when(source).getNodeInfo();
        doReturn(ClusterNode.builder().hostAndPort("localhost:10001").build()).when(target).getNodeInfo();
        doReturn(jedis).when(source).getJedis();
        doReturn(jedis).when(target).getJedis();
        doReturn(Lists.newArrayList("key1", "key2"))
                .doReturn(Lists.newArrayList())
                .when(jedis).clusterGetKeysInSlot(anyInt(), anyInt());

        // when
        tested.moveSlot(source, target, 0, Sets.newHashSet("cold", "update"));

        // then
        verify(source).getTmpSlots();
        verify(target).getTmpSlots();
    }

    @Test
    public void moveSlot_migrate_fail_then_throw_exception() {
        expectedEx.expect(ApiException.class);

        // given
        TribClusterNode source = mock(TribClusterNode.class);
        TribClusterNode target = mock(TribClusterNode.class);

        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(source, target));
        doReturn(ClusterNode.builder().hostAndPort("localhost:10000").build()).when(source).getNodeInfo();
        doReturn(ClusterNode.builder().hostAndPort("localhost:10001").build()).when(target).getNodeInfo();
        doReturn(jedis).when(source).getJedis();
        doReturn(jedis).when(target).getJedis();
        doReturn(Lists.newArrayList("key1", "key2"))
                .doReturn(Lists.newArrayList())
                .when(jedis).clusterGetKeysInSlot(anyInt(), anyInt());
        doThrow(Exception.class).when(jedis).migrate(anyString(), anyInt(), anyString(), anyInt(), anyInt());

        // then
        tested.moveSlot(source, target, 0, Sets.newHashSet("cold", "update"));
    }

    @Test(expected = InvalidParameterException.class)
    public void reshardCluster_invalid_slotCount() throws Exception {
        tested.reshardCluster("localhost:10080", -1, "ALL", "toNodeId");
    }

    @Test(expected = InvalidParameterException.class)
    public void reshardCluster_toNodeId_is_blank() throws Exception {
        tested.reshardCluster("localhost:10080", 10, "ALL", "");
    }

    @Test(expected = InvalidParameterException.class)
    public void reshardCluster_fromNodeId_is_blank() throws Exception {
        tested.reshardCluster("localhost:10080", 10, "", "toNodeId");
    }

    @Test
    public void reshardCluster_cluster_has_error() throws Exception {
        expectedEx.expect(ApiException.class);
        expectedEx.expectMessage(containsString("Please fix your cluster problems before resharding"));

        // given
        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        Whitebox.setInternalState(tested, "errors", Lists.newArrayList("error"));

        // when
        tested.reshardCluster("localhost:10080", 10, "ALL", "toNodeId");
    }

    @Test
    public void reshardCluster_ALL() throws Exception {
        // given
        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId1").build()).when(node1).getNodeInfo();
        doReturn(true).when(node1).hasFlag("slave");

        TribClusterNode node2 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId2").build()).when(node2).getNodeInfo();
        doReturn(false).when(node2).hasFlag("slave");

        TribClusterNode node3 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId3").build()).when(node3).getNodeInfo();
        doReturn(false).when(node3).hasFlag("slave");

        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(node1, node2, node3));
        doReturn(Lists.newArrayList()).when(tested).computeReshardTable(anyList(), anyInt());

        // when
        tested.reshardCluster("localhost:10080", 10, "ALL", "nodeId2");

        // then
        verify(tested).computeReshardTable(Lists.newArrayList(node3), 10);
    }

    @Test
    public void reshardCluster_specify_fromNodeIds_contains_slave() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("The specified node(nodeId1) is not known or is not a master, please retry"));

        // given
        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId1").build()).when(node1).getNodeInfo();
        doReturn(true).when(node1).hasFlag("slave");

        TribClusterNode node2 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId2").build()).when(node2).getNodeInfo();
        doReturn(false).when(node2).hasFlag("slave");

        TribClusterNode node3 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId3").build()).when(node3).getNodeInfo();
        doReturn(false).when(node3).hasFlag("slave");

        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(node1, node2, node3));
        doReturn(Lists.newArrayList()).when(tested).computeReshardTable(anyList(), anyInt());

        // when
        tested.reshardCluster("localhost:10080", 10, "nodeId1,nodeId3", "nodeId2");

        // then
        verify(tested).computeReshardTable(Lists.newArrayList(node3), 10);
    }

    @Test
    public void reshardCluster_specify_fromNodeIds_contains_toNodeId() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Target node is also listed among the source nodes"));

        // given
        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId1").build()).when(node1).getNodeInfo();
        doReturn(false).when(node1).hasFlag("slave");

        TribClusterNode node2 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId2").build()).when(node2).getNodeInfo();
        doReturn(false).when(node2).hasFlag("slave");

        TribClusterNode node3 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId3").build()).when(node3).getNodeInfo();
        doReturn(false).when(node3).hasFlag("slave");

        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(node1, node2, node3));
        doReturn(Lists.newArrayList()).when(tested).computeReshardTable(anyList(), anyInt());

        // when
        tested.reshardCluster("localhost:10080", 10, "nodeId1,nodeId2,nodeId3", "nodeId2");

        // then
        verify(tested).computeReshardTable(Lists.newArrayList(node3), 10);
    }

    @Test
    public void reshardCluster_specify_fromNodeIds() throws Exception {
        // given
        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId1").build()).when(node1).getNodeInfo();
        doReturn(false).when(node1).hasFlag("slave");

        TribClusterNode node2 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId2").build()).when(node2).getNodeInfo();
        doReturn(false).when(node2).hasFlag("slave");

        TribClusterNode node3 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder().nodeId("nodeId3").build()).when(node3).getNodeInfo();
        doReturn(false).when(node3).hasFlag("slave");

        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(node1, node2, node3));
        doReturn(Lists.newArrayList()).when(tested).computeReshardTable(anyList(), anyInt());

        // when
        tested.reshardCluster("localhost:10080", 10, "nodeId1,nodeId3", "nodeId2");

        // then
        verify(tested).computeReshardTable(Lists.newArrayList(node1, node3), 10);
    }

    @Test
    public void computeReshardTable() {
        // given
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

        // when
        List<RedisTrib.ReshardTable> result = tested.computeReshardTable(Lists.newArrayList(node1, node2, node3), 4);

        // then
        assertThat(result.get(0), is(new RedisTrib.ReshardTable(node1, 0)));
        assertThat(result.get(1), is(new RedisTrib.ReshardTable(node1, 1)));
        assertThat(result.get(2), is(new RedisTrib.ReshardTable(node2, 3)));
        assertThat(result.get(3), is(new RedisTrib.ReshardTable(node3, 6)));
    }

    @Test
    public void computeReshardTable_not_enough() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Total slot count which is sum of from nodes is not enough"));

        // given
        TribClusterNode node1 = mock(TribClusterNode.class);
        doReturn(ClusterNode.builder()
                .nodeId("nodeId1")
                .servedSlotsSet(Sets.newTreeSet(Sets.newHashSet(0, 1, 2)))
                .build())
                .when(node1).getNodeInfo();

        // when
        List<RedisTrib.ReshardTable> result = tested.computeReshardTable(Lists.newArrayList(node1), 4);
    }

    @Test
    public void reshardClusterBySlots_toNodeId_is_blank() throws Exception {
        expectedEx.expect(InvalidParameterException.class);

        // when
        tested.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(0, 1, 2)), "");
    }

    @Test
    public void reshardClusterBySlots_cluster_has_error() throws Exception {
        expectedEx.expect(ApiException.class);
        expectedEx.expectMessage(containsString("Please fix your cluster problems before resharding"));

        // given
        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        Whitebox.setInternalState(tested, "errors", Lists.newArrayList("error"));

        // when
        tested.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(0, 1, 2)), "toNodeId");
    }

    @Test
    public void reshardClusterBySlots_toNode_does_not_exist() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("The specified node is not known or is not a master"));

        // given
        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        doReturn(null).when(tested).getNodeByNodeId(anyString());

        // when
        tested.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(0, 1, 2)), "toNodeId");
    }

    @Test
    public void reshardClusterBySlots_toNode_is_slave() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("The specified node is not known or is not a master"));

        // given
        TribClusterNode targetNode = mock(TribClusterNode.class);
        doReturn(true).when(targetNode).hasFlag("slave");

        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        doReturn(targetNode).when(tested).getNodeByNodeId(anyString());

        // when
        tested.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(0, 1, 2)), "toNodeId");
    }

    @Test
    public void reshardClusterBySlots_notFoundSlot_exists() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Cannot find the nodes which has slots"));

        // given
        TribClusterNode targetNode = mock(TribClusterNode.class);
        doReturn(false).when(targetNode).hasFlag("slave");
        doReturn(ClusterNode.builder()
                .nodeId("targetNodeId")
                .servedSlotsSet(Sets.newTreeSet(Sets.newHashSet(0, 1, 2)))
                .build())
                .when(targetNode).getNodeInfo();

        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        doReturn(targetNode).when(tested).getNodeByNodeId(anyString());

        // when
        tested.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(2, 3, 4)), "toNodeId");
    }

    @Test
    public void reshardClusterBySlots() throws Exception {
        // given
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

        Whitebox.setInternalState(tested, "nodes", Lists.newArrayList(targetNode, node1, node2));
        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        doReturn(targetNode).when(tested).getNodeByNodeId(anyString());
        doNothing().when(tested).moveSlot(any(), any(), anyInt(), anySet());

        // when
        tested.reshardClusterBySlots("localhost:10080", Sets.newTreeSet(Sets.newHashSet(2, 3, 4)), "toNodeId");

        // then
        verify(tested).moveSlot(node1, targetNode, 3, null);
        verify(tested).moveSlot(node1, targetNode, 4, null);
    }

    @Test
    public void validateNewNode_newNode_is_not_connected() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("error"));

        // given
        doThrow(new InvalidParameterException("error")).when(tribClusterNode).connect(true);

        // when
        tested.validateClusterAndEmptyNode(tribClusterNode);
    }

    @Test
    public void validateNewNode_newNode_is_not_cluster() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("error"));

        // given
        doNothing().when(tribClusterNode).connect(true);
        doThrow(new InvalidParameterException("error")).when(tribClusterNode).assertCluster();

        // when
        tested.validateClusterAndEmptyNode(tribClusterNode);
    }

    @Test
    public void validateNewNode_newNode_is_not_empty() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("error"));

        // given
        doNothing().when(tribClusterNode).connect(true);
        doNothing().when(tribClusterNode).assertCluster();
        doThrow(new InvalidParameterException("error")).when(tribClusterNode).assertEmpty();

        // when
        tested.validateClusterAndEmptyNode(tribClusterNode);
    }

    @Test
    public void addNodeIntoCluster_invalid_newHostAndPort() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Invalid parameter. Node is invalid."));

        // when
        tested.addNodeIntoCluster("localhost:10080", "aaaaaaaaaaa");
    }

    @Test
    public void addNodeIntoClusterAsReplica_masterId_does_not_exists() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("No such master ID"));

        // given
        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        doReturn(null).when(tested).getNodeByNodeId(anyString());

        // when
        tested.addNodeIntoClusterAsReplica("localhost:10080", "localhost:10081", "masterNodeId");
    }

    @Test
    public void addNodeIntoClusterAsReplica_masterId_is_RANDOM() throws Exception {
        // given
        doReturn(ClusterNode.builder().hostAndPort("localhost:10000").build()).when(tribClusterNode).getNodeInfo();
        doReturn(jedis).when(tribClusterNode).getJedis();

        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).checkCluster();
        doReturn(tribClusterNode).when(tested).getMasterWithLeastReplicas();
        doReturn(tribClusterNode).when(tested).createTribClusterNode(anyString());
        doReturn(tribClusterNode).when(tested).getNodeByHostAndPort(anyString());

        // when
        tested.addNodeIntoClusterAsReplica("localhost:10080", "localhost:10081", "RANDOM");

        // then
        verify(tested).getMasterWithLeastReplicas();
    }

    @Test
    public void deleteNodeOfCluster_nodeId_does_not_exists() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("No such node ID"));

        // given
        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(null).when(tested).getNodeByNodeId(anyString());

        // when
        tested.deleteNodeOfCluster("localhost:10080", "nodeId", null, false);
    }

    @Test
    public void deleteNodeOfCluster_not_empty_node() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("is not empty"));

        // given
        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doReturn(tribClusterNode).when(tested).getNodeByNodeId(anyString());
        doReturn(ClusterNode.builder().servedSlotsSet(Sets.newHashSet(0)).build()).when(tribClusterNode).getNodeInfo();
        doNothing().when(tested).forgotNode(anyString());
        doReturn(jedis).when(tribClusterNode).getJedis();

        // when
        tested.deleteNodeOfCluster("localhost:10080", "nodeId", null, false);
    }

    @Test
    public void deleteFailNodeOfCluster_nodeId() throws Exception {
        // given
        doNothing().when(tested).loadClusterInfoFromNode(anyString());
        doNothing().when(tested).forgotNode(anyString());

        // when
        tested.deleteFailNodeOfCluster("localhost:10080", "nodeId");
    }
}
