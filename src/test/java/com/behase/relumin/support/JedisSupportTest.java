package com.behase.relumin.support;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.ClusterNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Spy;
import org.springframework.boot.test.OutputCapture;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@Slf4j
public class JedisSupportTest {
    @Spy
    private JedisSupport jedisSupport = new JedisSupport();

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Test
    public void getJedisByHostAndPort() {
        assertThat(jedisSupport.getJedisByHostAndPort("localhost:10000", 2000), is(instanceOf(Jedis.class)));
        assertThat(jedisSupport.getJedisByHostAndPort("localhost:10000"), is(instanceOf(Jedis.class)));
    }

    @Test
    public void getJedisClusterByHostAndPort() {
        assertThat(jedisSupport.getJedisClusterByHostAndPort("localhost:10000"), is(instanceOf(JedisCluster.class)));
        assertThat(jedisSupport.getJedisClusterByHostAndPorts("localhost:10000,localhost:10001-10005"), is(instanceOf(JedisCluster.class)));
    }

    @Test
    public void parseInfoResult() {
        String infoText = "" +
                "# Server\n" +
                "redis_version:3.0.3\n" +
                "redis_git_sha1:00000000\n" +
                "redis_git_dirty:0";

        Map<String, String> result = jedisSupport.parseInfoResult(infoText);
        log.info("result={}", result);
        assertThat(result, hasKey("_timestamp"));
        assertThat(result, hasEntry("redis_version", "3.0.3"));
        assertThat(result, hasEntry("redis_git_dirty", "0"));
    }

    @Test
    public void parseClusterInfoResult() {
        String clusterInfoText = "" +
                "cluster_state:fail\n" +
                "cluster_slots_assigned:0\n" +
                "cluster_slots_ok:0";

        Map<String, String> result = jedisSupport.parseClusterInfoResult(clusterInfoText);
        log.info("result={}", result);
        assertThat(result, hasEntry("cluster_state", "fail"));
        assertThat(result, hasEntry("cluster_slots_assigned", "0"));
        assertThat(result, hasEntry("cluster_slots_ok", "0"));
    }

    @Test
    public void parseClusterNodesResult() {
        String clusterNodesText = "" +
                "7893f01887835a6e19b09ff663909fced0744926 127.0.0.1:7001 myself,master - 0 0 1 connected 0-2000 2001-4094 4095 [93-<-292f8b365bb7edb5e285caf0b7e6ddc7265d2f4f] [77->-e7d1eecce10fd6bb5eb35b9f99a514335d9ba9ca]\n" +
                "9bd5a779d5981cee7d561dc2bfc984ffbfc744d3 10.128.214.37:7002 slave 4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2 0 1459242326643 5 disconnected\n" +
                "c3c0b2b7d7d50e339565de468e7ebd7db79a1ea5 10.128.214.37:7003 master - 0 1459242325640 3 connected 8192-12287\n" +
                "20e7c57506199c468b0672fda7b00d12a2d6a547 10.128.214.37:7004 slave a4f318b3fb0affd5d130b29cb6161a7e225216b5 0 1459242324639 6 connected\n" +
                "8e309bc36225a6bfd46ede7ff377b54e0bdbfc5d 10.128.214.38:7001 slave 7893f01887835a6e19b09ff663909fced0744926 0 1459242328644 1 connected\n" +
                "4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2 10.128.214.38:7002 master - 0 1459242323638 5 connected 4096-8191\n" +
                "7040f0339855ff0faf1abeb32baad0d6441e8e2f 10.128.214.38:7003 slave c3c0b2b7d7d50e339565de468e7ebd7db79a1ea5 0 1459242327643 3 connected\n" +
                "a4f318b3fb0affd5d130b29cb6161a7e225216b5 10.128.214.38:7004 master - 0 1459242328644 6 connected 12288-16383";

        List<ClusterNode> result;

        result = jedisSupport.parseClusterNodesResult(clusterNodesText, "");
        log.info("result={}", result);
        assertThat(result.get(0).getNodeId(), is("7893f01887835a6e19b09ff663909fced0744926"));
        assertThat(result.get(0).getHost(), is("127.0.0.1"));
        assertThat(result.get(0).getFlags(), contains("myself", "master"));
        assertThat(result.get(1).getFlags(), contains("slave"));
        assertThat(result.get(0).getMasterNodeId(), is(""));
        assertThat(result.get(1).getMasterNodeId(), is("4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2"));
        assertThat(result.get(0).getPingSent(), is(0L));
        assertThat(result.get(0).getPongReceived(), is(0L));
        assertThat(result.get(0).getConfigEpoch(), is(1L));
        assertThat(result.get(0).isConnect(), is(true));
        assertThat(result.get(1).isConnect(), is(false));
        assertThat(result.get(0).getServedSlots(), is("0-4095"));
        assertThat(result.get(0).getImporting(), hasEntry(93, "292f8b365bb7edb5e285caf0b7e6ddc7265d2f4f"));
        assertThat(result.get(0).getMigrating(), hasEntry(77, "e7d1eecce10fd6bb5eb35b9f99a514335d9ba9ca"));
        assertThat(result.get(1).getServedSlots(), is(""));

        result = jedisSupport.parseClusterNodesResult(clusterNodesText, "10.128.214.37:7001");
        log.info("result={}", result);
        assertThat(result.get(0).getHost(), is("10.128.214.37"));
    }

    @Test
    public void slotsDisplay() {
        Set<Integer> slots = Sets.newTreeSet();

        slots.clear();
        assertThat(jedisSupport.slotsDisplay(slots), is(""));

        slots.clear();
        slots.add(10);
        assertThat(jedisSupport.slotsDisplay(slots), is("10"));

        slots.clear();
        IntStream.rangeClosed(0, 10).forEach(i -> slots.add(i));
        slots.add(50);
        IntStream.rangeClosed(100, 110).forEach(i -> slots.add(i));
        slots.add(200);
        slots.add(300);
        assertThat(jedisSupport.slotsDisplay(slots), is("0-10,50,100-110,200,300"));

        slots.clear();
        slots.add(50);
        IntStream.rangeClosed(100, 110).forEach(i -> slots.add(i));
        assertThat(jedisSupport.slotsDisplay(slots), is("50,100-110"));

        slots.clear();
        IntStream.rangeClosed(0, 10).forEach(i -> slots.add(i));
        slots.add(50);
        IntStream.rangeClosed(100, 110).forEach(i -> slots.add(i));
        slots.add(200);
        assertThat(jedisSupport.slotsDisplay(slots), is("0-10,50,100-110,200"));
    }

    @Test
    public void getHostAndPorts_invalid_hostAndPorts_range_then_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Node is invalid"));

        jedisSupport.getHostAndPorts(Lists.newArrayList("localhost:1000", "localhost"));
    }

    @Test
    public void getHostAndPorts_end_port_is_less_than_start_port_than_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("start port must be equal or less than end port"));

        jedisSupport.getHostAndPorts(Lists.newArrayList("localhost:1000", "localhost:2001-2000"));
    }

    @Test
    public void getHostAndPorts() {
        Set<String> hostAndPorts = jedisSupport.getHostAndPorts(Lists.newArrayList("localhost:1000", "localhost:2000-2002"));
        assertThat(hostAndPorts, contains("localhost:1000", "localhost:2000", "localhost:2001", "localhost:2002"));
    }

    @Test
    public void getSlots_invalid_start_slot_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Slot must be numeric"));

        jedisSupport.getSlots(Lists.newArrayList("hoge-3"));
    }

    @Test
    public void getSlots_invalid_end_slot_throw_exception() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Slot must be numeric"));

        jedisSupport.getSlots(Lists.newArrayList("0-hoge"));
    }

    @Test
    public void getSlots() {
        Set<Integer> result = jedisSupport.getSlots(Lists.newArrayList("0-3", "5"));
        log.info("result={}", result);
        assertThat(result, contains(0, 1, 2, 3, 5));
    }
}
