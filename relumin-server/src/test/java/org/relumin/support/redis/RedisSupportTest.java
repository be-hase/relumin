package org.relumin.support.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.Test;

import org.relumin.model.ClusterNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class RedisSupportTest {
    private RedisSupport target = new RedisSupport();

    @Test
    public void parseInfoResult() {
        final String infoText = "" +
                                "# Server\n" +
                                "redis_version:3.0.3\n" +
                                "redis_git_sha1:00000000\n" +
                                "redis_git_dirty:0";

        final Map<String, String> result = target.parseInfoResult(infoText);

        assertThat(result).containsKey("_timestamp");
        assertThat(result).containsEntry("redis_version", "3.0.3");
        assertThat(result).containsEntry("redis_git_sha1", "00000000");
        assertThat(result).containsEntry("redis_git_dirty", "0");
    }

    @Test
    public void parseClusterInfoResult() {
        final String clusterInfoText = "" +
                                       "cluster_state:fail\n" +
                                       "cluster_slots_assigned:0\n" +
                                       "cluster_slots_ok:0";

        final Map<String, String> result = target.parseClusterInfoResult(clusterInfoText);

        assertThat(result).containsEntry("cluster_state", "fail");
        assertThat(result).containsEntry("cluster_slots_assigned", "0");
        assertThat(result).containsEntry("cluster_slots_ok", "0");
    }

    @Test
    public void parseClusterNodesResult() {
        final String clusterNodesText = "" +
                                        "7893f01887835a6e19b09ff663909fced0744926 127.0.0.1:7001 myself,master - 0 0 1 connected 0-2000 2001-4094 4095 [93-<-292f8b365bb7edb5e285caf0b7e6ddc7265d2f4f] [77->-e7d1eecce10fd6bb5eb35b9f99a514335d9ba9ca]\n"
                                        +
                                        "9bd5a779d5981cee7d561dc2bfc984ffbfc744d3 192.168.99.100:7002 slave 4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2 0 1459242326643 5 disconnected\n"
                                        +
                                        "c3c0b2b7d7d50e339565de468e7ebd7db79a1ea5 192.168.99.100:7003 master - 0 1459242325640 3 connected 8192-12287\n"
                                        +
                                        "20e7c57506199c468b0672fda7b00d12a2d6a547 192.168.99.100:7004 slave a4f318b3fb0affd5d130b29cb6161a7e225216b5 0 1459242324639 6 connected\n"
                                        +
                                        "8e309bc36225a6bfd46ede7ff377b54e0bdbfc5d 192.168.99.101:7001 slave 7893f01887835a6e19b09ff663909fced0744926 0 1459242328644 1 connected\n"
                                        +
                                        "4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2 192.168.99.101:7002 master - 0 1459242323638 5 connected 4096-8191\n"
                                        +
                                        "7040f0339855ff0faf1abeb32baad0d6441e8e2f 192.168.99.101:7003 slave c3c0b2b7d7d50e339565de468e7ebd7db79a1ea5 0 1459242327643 3 connected\n"
                                        +
                                        "a4f318b3fb0affd5d130b29cb6161a7e225216b5 192.168.99.101:7004 master - 0 1459242328644 6 connected 12288-16383";

        List<ClusterNode> result;

        result = target.parseClusterNodesResult(clusterNodesText, "192.168.99.100:7001");

        assertThat(result.get(0).getNodeId()).isEqualTo("7893f01887835a6e19b09ff663909fced0744926");
        assertThat(result.get(0).getHost()).isEqualTo("192.168.99.100");
        assertThat(result.get(0).getFlags()).contains("myself", "master");
        assertThat(result.get(1).getFlags()).contains("slave");
        assertThat(result.get(0).getMasterNodeId()).isEqualTo("");
        assertThat(result.get(1).getMasterNodeId()).isEqualTo("4e97c7f8fc08d2bb3e45571c4f001a7a347cbbe2");
        assertThat(result.get(0).getPingSent()).isEqualTo(0L);
        assertThat(result.get(0).getPongReceived()).isEqualTo(0L);
        assertThat(result.get(0).getConfigEpoch()).isEqualTo(1L);
        assertThat(result.get(0).isConnected()).isTrue();
        assertThat(result.get(1).isConnected()).isFalse();
        assertThat(result.get(0).getServedSlots()).isEqualTo("0-4095");
        assertThat(result.get(0).getImporting()).containsEntry(93, "292f8b365bb7edb5e285caf0b7e6ddc7265d2f4f");
        assertThat(result.get(0).getMigrating()).containsEntry(77, "e7d1eecce10fd6bb5eb35b9f99a514335d9ba9ca");
        assertThat(result.get(1).getServedSlots()).isEqualTo("");

        result = target.parseClusterNodesResult(clusterNodesText, "192.168.99.100:7001");

        assertThat(result.get(0).getHost()).isEqualTo("192.168.99.100");
    }

    @Test
    public void slotsDisplay() {
        final Set<Integer> slots = Sets.newTreeSet();

        slots.clear();
        assertThat(target.slotsDisplay(slots)).isEqualTo("");

        slots.clear();
        slots.add(10);
        assertThat(target.slotsDisplay(slots)).isEqualTo("10");

        slots.clear();
        IntStream.rangeClosed(0, 10).forEach(i -> slots.add(i));
        slots.add(50);
        IntStream.rangeClosed(100, 110).forEach(i -> slots.add(i));
        slots.add(200);
        slots.add(300);
        assertThat(target.slotsDisplay(slots)).isEqualTo("0-10,50,100-110,200,300");

        slots.clear();
        slots.add(50);
        IntStream.rangeClosed(100, 110).forEach(i -> slots.add(i));
        assertThat(target.slotsDisplay(slots)).isEqualTo("50,100-110");

        slots.clear();
        IntStream.rangeClosed(0, 10).forEach(i -> slots.add(i));
        slots.add(50);
        IntStream.rangeClosed(100, 110).forEach(i -> slots.add(i));
        slots.add(200);
        assertThat(target.slotsDisplay(slots)).isEqualTo("0-10,50,100-110,200");
    }

    @Test
    public void getSlots() {
        Set<Integer> result = target.getSlots(Lists.newArrayList("0-3", "5"));
        assertThat(result).containsExactly(0, 1, 2, 3, 5);
    }

    @Test
    public void getSlotsInvalidStartSlotThrowException() {
        assertThatThrownBy(() -> target.getSlots(Lists.newArrayList("hoge-3")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'hoge-3' is invalid format.");
    }

    @Test
    public void getSlotsInvalidEndSlotThrowException() {
        assertThatThrownBy(() -> target.getSlots(Lists.newArrayList("3-hoge")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'3-hoge' is invalid format.");
    }

    @Test
    public void getSlotsInvalidStartAndEndThrowException() {
        assertThatThrownBy(() -> target.getSlots(Lists.newArrayList("3-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'3-1' is invalid format. start slot must be equal or less than end slot.");
    }
}
