package com.behase.relumin.service;

import com.behase.relumin.exception.ApiException;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.*;
import com.behase.relumin.support.JedisSupport;
import com.behase.relumin.webconfig.WebConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.OutputCapture;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class ClusterServiceImplTest {
    @InjectMocks
    @Spy
    private ClusterServiceImpl service = new ClusterServiceImpl();

    @Mock
    private JedisPool dataStoreJedisPool;

    @Spy
    private ObjectMapper mapper = WebConfig.MAPPER;

    @Mock
    private NodeService nodeService;

    @Mock
    private JedisSupport jedisSupport;

    @Mock
    private Jedis dataStoreJedis;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Before
    public void init() {
        Whitebox.setInternalState(service, "redisPrefixKey", "_relumin");
        doReturn(dataStoreJedis).when(dataStoreJedisPool).getResource();
    }

    @Test
    public void getClusters() throws Exception {
        doReturn(Sets.newHashSet("hoge", "bar")).when(dataStoreJedis).smembers(anyString());
        assertThat(service.getClusters(), contains("hoge", "bar"));
    }

    @Test
    public void getCluster() throws Exception {
        doReturn(ClusterNode.builder()
                .nodeId("nodeId1")
                .build()).when(service).getActiveClusterNode(anyString());
        doReturn(Cluster.builder()
                .info(Maps.newHashMap())
                .nodes(Lists.newArrayList())
                .build()).when(service).getClusterByHostAndPort(anyString());

        Cluster result = service.getCluster("cluster1");
        log.info("result={}", result);
        assertThat(result.getClusterName(), is("cluster1"));
    }

    @Test
    public void getClusterByHostAndPort() throws Exception {
        Map<String, String> info = Maps.newHashMap();
        info.put("cluster_state", "ok");
        info.put("cluster_size", "4");
        info.put("cluster_current_epoch", "1");

        List<ClusterNode> nodes = Lists.newArrayList(
                ClusterNode.builder()
                        .nodeId("nodeId1")
                        .hostAndPort("localhost:10000")
                        .flags(Sets.newHashSet("master"))
                        .masterNodeId("")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(IntStream.rangeClosed(0, 16382).mapToObj(i -> i).collect(Collectors.toSet()))
                        .build(),
                ClusterNode.builder()
                        .nodeId("nodeId2")
                        .hostAndPort("localhost:10001")
                        .flags(Sets.newHashSet("slave"))
                        .masterNodeId("nodeId1")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(Sets.newHashSet())
                        .build(),
                ClusterNode.builder()
                        .nodeId("nodeId3")
                        .hostAndPort("localhost:10002")
                        .flags(Sets.newHashSet("master"))
                        .masterNodeId("")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(Sets.newHashSet(16383))
                        .build(),
                ClusterNode.builder()
                        .nodeId("nodeId4")
                        .hostAndPort("localhost:10003")
                        .flags(Sets.newHashSet("slave"))
                        .masterNodeId("nodeId3")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(Sets.newHashSet())
                        .build()
        );

        doReturn(mock(Jedis.class)).when(jedisSupport).getJedisByHostAndPort(anyString());
        doReturn(info).when(jedisSupport).parseClusterInfoResult(anyString());
        doReturn(nodes).when(jedisSupport).parseClusterNodesResult(anyString(), any());

        Cluster result = service.getClusterByHostAndPort("");
        SlotInfo slotInfo1 = result.getSlots().get(0);
        SlotInfo slotInfo2 = result.getSlots().get(1);
        assertThat(result.getClusterName(), is(nullValue()));
        assertThat(result.getStatus(), is("ok"));
        assertThat(result.getInfo(), is(info));
        assertThat(result.getNodes(), is(nodes));
        assertThat(slotInfo1.getStartSlotNumber(), is(0));
        assertThat(slotInfo1.getEndSlotNumber(), is(16382));
        assertThat(slotInfo1.getMaster(), is(ImmutableMap.of("node_id", "nodeId1", "host_and_port", "localhost:10000")));
        assertThat(slotInfo1.getReplicas(), is(Lists.newArrayList(ImmutableMap.of("node_id", "nodeId2", "host_and_port", "localhost:10001"))));
        assertThat(slotInfo2.getStartSlotNumber(), is(16383));
        assertThat(slotInfo2.getEndSlotNumber(), is(16383));
        assertThat(slotInfo2.getMaster(), is(ImmutableMap.of("node_id", "nodeId3", "host_and_port", "localhost:10002")));
        assertThat(slotInfo2.getReplicas(), is(Lists.newArrayList(ImmutableMap.of("node_id", "nodeId4", "host_and_port", "localhost:10003"))));
    }

    @Test
    public void existsClusterName() throws Exception {
        doReturn(Sets.newHashSet("hoge", "bar")).when(dataStoreJedis).smembers(anyString());
        assertThat(service.existsClusterName("hoge"), is(true));
        assertThat(service.existsClusterName("hogehoge"), is(false));
    }

    @Test
    public void setCluster_clusterName_is_invalid_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("ClusterName is invalid"));

        service.setCluster("AA!!", "localhost:10000");
    }

    @Test
    public void setCluster_hostAndPort_is_invalid_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Node is invalid"));

        service.setCluster("clusterName", "localhost");
    }

    @Test
    public void setCluster_hostAndPort_cannot_be_connected_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Failed to connect to Redis Cluster"));

        Jedis jedis = mock(Jedis.class);

        doThrow(Exception.class).when(jedis).ping();
        doReturn(jedis).when(jedisSupport).getJedisByHostAndPort(anyString());

        service.setCluster("clusterName", "localhost:10000");
    }

    @Test
    public void setCluster_redis_is_not_cluster_mode_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("is not cluster mode"));

        Jedis jedis = mock(Jedis.class);
        Map<String, String> info = ImmutableMap.of("cluster_enabled", "0");

        doReturn(jedis).when(jedisSupport).getJedisByHostAndPort(anyString());
        doReturn(info).when(jedisSupport).parseInfoResult(anyString());

        service.setCluster("clusterName", "localhost:10000");
    }

    @Test
    public void setCluster() throws Exception {
        Map<String, String> info = ImmutableMap.of("cluster_enabled", "1");

        doReturn(mock(Jedis.class)).when(jedisSupport).getJedisByHostAndPort(anyString());
        doReturn(info).when(jedisSupport).parseInfoResult(anyString());
        doReturn(Lists.newArrayList()).when(jedisSupport).parseClusterNodesResult(anyString(), anyString());
        doReturn(mock(Pipeline.class)).when(dataStoreJedis).pipelined();

        service.setCluster("clusterName", "localhost:10000");
    }

    @Test
    public void changeClusterName_clusterName_is_invalid_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("ClusterName is invalid"));

        service.changeClusterName("clusterName", "newClusterName!!");
    }

    @Test
    public void changeClusterName_clusterName_does_not_exist_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("does not exists"));

        doReturn(false).when(service).existsClusterName("clusterName");

        service.changeClusterName("clusterName", "newClusterName");
    }

    @Test
    public void changeClusterName_newClusterName_already_exist_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("already exists"));

        doReturn(true).when(service).existsClusterName("clusterName");
        doReturn(true).when(service).existsClusterName("newClusterName");

        service.changeClusterName("clusterName", "newClusterName");
    }

    @Test
    public void changeClusterName() throws Exception {
        doReturn(true).when(service).existsClusterName("clusterName");
        doReturn(false).when(service).existsClusterName("newClusterName");
        doReturn(mock(Pipeline.class)).when(dataStoreJedis).pipelined();

        service.changeClusterName("clusterName", "newClusterName");
    }

    @Test
    public void getClusterNotice() throws Exception {
        Notice notice = new Notice();
        notice.setInvalidEndTime(String.valueOf(System.currentTimeMillis()));

        doReturn(mapper.writeValueAsString(notice)).when(dataStoreJedis).get(anyString());

        Notice result = service.getClusterNotice("clusterName");
        log.info("result={}", result);
        assertThat(result, is(notice));
    }

    @Test
    public void setClusterNotice() throws Exception {
        Notice notice = new Notice();
        notice.setInvalidEndTime(String.valueOf(System.currentTimeMillis()));

        service.setClusterNotice("clusterName", notice);
    }

    @Test
    public void deleteCluster() throws Exception {
        doReturn(Sets.newHashSet("hoge, bar")).when(dataStoreJedis).keys(anyString());
        doReturn(mock(Pipeline.class)).when(dataStoreJedis).pipelined();

        service.deleteCluster("clusterName");
    }

    @Test
    public void refreshClusters() throws Exception {
        ClusterNode clusterNode = new ClusterNode();
        Jedis jedis = mock(Jedis.class);
        List<ClusterNode> nodes = Lists.newArrayList(
                ClusterNode.builder()
                        .nodeId("nodeId1")
                        .flags(Sets.newHashSet())
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(Sets.newHashSet())
                        .build(),
                ClusterNode.builder()
                        .nodeId("nodeId2")
                        .flags(Sets.newHashSet())
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(Sets.newHashSet())
                        .build()
        );

        doReturn(Sets.newHashSet("hoge")).when(service).getClusters();
        doReturn(clusterNode).when(service).getActiveClusterNode("hoge");
        doReturn(jedis).when(jedisSupport).getJedisByHostAndPort(anyString());
        doReturn(nodes).when(jedisSupport).parseClusterNodesResult(anyString(), anyString());
        doReturn(Sets.newHashSet("nodeId1", "nodeId2", "nodeId3")).when(dataStoreJedis).keys(anyString());

        service.refreshClusters();

        String output = capture.toString();
        assertThat(output, containsString("delete extra data node. key : _relumin.cluster.hoge.node.nodeId3.staticsInfo"));
    }

    @Test
    public void getActiveClusterNodeWithExcludePredicate_predicate_is_null() throws Exception {
        List<ClusterNode> nodes = Lists.newArrayList(
                ClusterNode.builder()
                        .nodeId("nodeId1")
                        .hostAndPort("localhost:10000")
                        .flags(Sets.newHashSet("master"))
                        .masterNodeId("")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(IntStream.rangeClosed(0, 16382).mapToObj(i -> i).collect(Collectors.toSet()))
                        .build(),
                ClusterNode.builder()
                        .nodeId("nodeId2")
                        .hostAndPort("localhost:10001")
                        .flags(Sets.newHashSet("slave"))
                        .masterNodeId("nodeId1")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(Sets.newHashSet())
                        .build(),
                ClusterNode.builder()
                        .nodeId("nodeId3")
                        .hostAndPort("localhost:10002")
                        .flags(Sets.newHashSet("master"))
                        .masterNodeId("")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(Sets.newHashSet(16383))
                        .build(),
                ClusterNode.builder()
                        .nodeId("nodeId4")
                        .hostAndPort("localhost:10003")
                        .flags(Sets.newHashSet("slave"))
                        .masterNodeId("nodeId3")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(Sets.newHashSet())
                        .build()
        );

        Jedis upJedis = mock(Jedis.class);
        Jedis downJedis = mock(Jedis.class);

        doReturn(mapper.writeValueAsString(nodes)).when(dataStoreJedis).get(anyString());
        doReturn(upJedis).when(jedisSupport).getJedisByHostAndPort("localhost:10000");
        doReturn(downJedis).when(jedisSupport).getJedisByHostAndPort("localhost:10001");
        doReturn(downJedis).when(jedisSupport).getJedisByHostAndPort("localhost:10002");
        doReturn(downJedis).when(jedisSupport).getJedisByHostAndPort("localhost:10003");
        doReturn("PONG").when(upJedis).ping();
        doThrow(JedisException.class).when(downJedis).ping();

        ClusterNode result = service.getActiveClusterNodeWithExcludePredicate("clusterName", null);
        log.info("result={}", result.getNodeId());
        assertThat(result.getNodeId(), is("nodeId1"));
    }

    @Test
    public void getActiveClusterNodeWithExcludePredicate() throws Exception {
        List<ClusterNode> nodes = Lists.newArrayList(
                ClusterNode.builder()
                        .nodeId("nodeId1")
                        .hostAndPort("localhost:10000")
                        .flags(Sets.newHashSet("master"))
                        .masterNodeId("")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(IntStream.rangeClosed(0, 16382).mapToObj(i -> i).collect(Collectors.toSet()))
                        .build(),
                ClusterNode.builder()
                        .nodeId("nodeId2")
                        .hostAndPort("localhost:10001")
                        .flags(Sets.newHashSet("slave"))
                        .masterNodeId("nodeId1")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(Sets.newHashSet())
                        .build(),
                ClusterNode.builder()
                        .nodeId("nodeId3")
                        .hostAndPort("localhost:10002")
                        .flags(Sets.newHashSet("master"))
                        .masterNodeId("")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(Sets.newHashSet(16383))
                        .build(),
                ClusterNode.builder()
                        .nodeId("nodeId4")
                        .hostAndPort("localhost:10003")
                        .flags(Sets.newHashSet("slave"))
                        .masterNodeId("nodeId3")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(Sets.newHashSet())
                        .build()
        );

        Jedis jedis = mock(Jedis.class);

        doReturn(mapper.writeValueAsString(nodes)).when(dataStoreJedis).get(anyString());
        doReturn(jedis).when(jedisSupport).getJedisByHostAndPort(anyString());
        doReturn("PONG").when(jedis).ping();

        assertThat(
                service.getActiveClusterNodeWithExcludePredicate(
                        "clusterName",
                        clusterNode -> StringUtils.equalsIgnoreCase(clusterNode.getHostAndPort(), "localhost:10000")
                ).getNodeId(),
                anyOf(is("nodeId2"), is("nodeId3"), is("nodeId4"))
        );
        assertThat(
                service.getActiveClusterNodeWithExcludePredicate(
                        "clusterName",
                        clusterNode -> StringUtils.equalsIgnoreCase(clusterNode.getNodeId(), "nodeId1")
                ).getNodeId(),
                anyOf(is("nodeId2"), is("nodeId3"), is("nodeId4"))
        );
    }

    @Test
    public void getActiveClusterNodeWithExcludePredicate_allNode_is_down() throws Exception {
        expectedEx.expect(ApiException.class);
        expectedEx.expectMessage(containsString("All node is down"));

        List<ClusterNode> nodes = Lists.newArrayList(
                ClusterNode.builder()
                        .nodeId("nodeId1")
                        .hostAndPort("localhost:10000")
                        .flags(Sets.newHashSet("master"))
                        .masterNodeId("")
                        .pingSent(System.currentTimeMillis())
                        .pongReceived(System.currentTimeMillis())
                        .configEpoch(1)
                        .connect(true)
                        .migrating(Maps.newHashMap())
                        .importing(Maps.newHashMap())
                        .servedSlotsSet(IntStream.rangeClosed(0, 16383).mapToObj(i -> i).collect(Collectors.toSet()))
                        .build()
        );

        Jedis jedis = mock(Jedis.class);

        doReturn(mapper.writeValueAsString(nodes)).when(dataStoreJedis).get(anyString());
        doReturn(jedis).when(jedisSupport).getJedisByHostAndPort(anyString());
        doThrow(JedisException.class).when(jedis).ping();

        service.getActiveClusterNodeWithExcludePredicate("clusterName", null);
    }

    @Test
    public void getActiveClusterNodeWithExcludePredicate_clusterName_does_not_exist() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Not exists this cluster name"));

        doReturn(null).when(dataStoreJedis).get(anyString());

        service.getActiveClusterNodeWithExcludePredicate("clusterName", null);
    }

    @Test
    public void getClusterStaticsInfoHistory_end_is_smaller_than_start() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("End time must be larger than start time"));

        service.getClusterStaticsInfoHistory("clusterName", null, null, 10, 1);
    }

    @Test
    public void getClusterStaticsInfoHistory() throws Exception {
        Map<String, List<List<Object>>> staticsInfoHistory = Maps.newHashMap();
        staticsInfoHistory.put("instantaneous_ops_per_sec", Lists.newArrayList(
                Lists.newArrayList(System.currentTimeMillis(), 0),
                Lists.newArrayList(System.currentTimeMillis(), 1)
        ));

        doReturn(staticsInfoHistory).when(nodeService).getStaticsInfoHistory(anyString(), anyString(), anyList(), anyLong(), anyLong());

        Map<String, Map<String, List<List<Object>>>> result = service.getClusterStaticsInfoHistory(
                "clusterName",
                Lists.newArrayList("nodeId1", "nodeId2"),
                Lists.newArrayList("instantaneous_ops_per_sec"),
                1,
                10
        );
        log.info("result={}", result);
        assertThat(result, is(ImmutableMap.of("nodeId1", staticsInfoHistory, "nodeId2", staticsInfoHistory)));
    }

    @Test
    public void getClusterSlowLogHistory() throws Exception {
        // given
        List<String> strList = Lists.newArrayList(
                mapper.writeValueAsString(SlowLog.builder().id(3L).timeStamp(3L).build()),
                mapper.writeValueAsString(SlowLog.builder().id(2L).timeStamp(2L).build()),
                mapper.writeValueAsString(SlowLog.builder().id(1L).timeStamp(1L).build())
        );
        doReturn(strList).when(dataStoreJedis).lrange(anyString(), anyLong(), anyLong());
        doReturn(10L).when(dataStoreJedis).llen(anyString());

        // when
        PagerData<SlowLog> pagerData = service.getClusterSlowLogHistory("clusterName", 0L, 3L);
        log.info("result={}", mapper.writeValueAsString(pagerData));

        assertThat(pagerData.getOffset(), is(0L));
        assertThat(pagerData.getLimit(), is(3L));
        assertThat(pagerData.getTotal(), is(10L));
        assertThat(pagerData.getData().size(), is(3));
        assertThat(pagerData.getCurrentPage(), is(1L));
        assertThat(pagerData.getLastPage(), is(4L));
    }
}
