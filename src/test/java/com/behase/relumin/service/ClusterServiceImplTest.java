package com.behase.relumin.service;

import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.model.SlotInfo;
import com.behase.relumin.support.JedisSupport;
import com.behase.relumin.webconfig.WebConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@Slf4j
public class ClusterServiceImplTest {
    private ClusterServiceImpl service;
    private JedisPool dataStoreJedisPool;
    private ObjectMapper mapper;
    private NodeService nodeService;
    private JedisSupport jedisSupport;
    private String redisPrefixKey;
    private Jedis dataStoreJedis;

    @Before
    public void init() {
        service = spy(new ClusterServiceImpl());
        dataStoreJedisPool = mock(JedisPool.class);
        mapper = WebConfig.MAPPER;
        nodeService = mock(NodeService.class);
        jedisSupport = mock(JedisSupport.class);
        redisPrefixKey = "_relumin";
        dataStoreJedis = mock(Jedis.class);

        inject();
    }

    private void inject() {
        Whitebox.setInternalState(service, "dataStoreJedisPool", dataStoreJedisPool);
        Whitebox.setInternalState(service, "mapper", mapper);
        Whitebox.setInternalState(service, "nodeService", nodeService);
        Whitebox.setInternalState(service, "jedisSupport", jedisSupport);
        Whitebox.setInternalState(service, "redisPrefixKey", redisPrefixKey);

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
                        .build()
        );
        log.info("result={}", nodes.get(0).getServedSlots());

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
    public void existsClusterName() {
        doReturn(Sets.newHashSet("hoge", "bar")).when(dataStoreJedis).smembers(anyString());
        assertThat(service.existsClusterName("hoge"), is(true));
        assertThat(service.existsClusterName("hogehoge"), is(false));
    }
}
