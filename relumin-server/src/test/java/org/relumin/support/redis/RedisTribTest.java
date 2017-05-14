package org.relumin.support.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.relumin.Application;
import org.relumin.model.ClusterNode;
import org.relumin.model.CreateClusterParam;
import org.relumin.test.TestHelper;
import org.relumin.test.TestRedisProperties;
import org.relumin.test.TestRedisProperties.RedisUri;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.junit4.SpringRunner;

import com.lambdaworks.redis.RedisURI;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class RedisTribTest {
    @Autowired
    private TestRedisProperties testRedisProperties;

    @Autowired
    private RedisSupport redisSupport;

    @Autowired
    private TestHelper testHelper;

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Before
    public void resetAllRedis() {
        testHelper.resetAllRedis();
    }

    private void executeRedisTrib(final Consumer<RedisTrib> consumer) {
        try (final RedisTrib redisTrib = new RedisTrib()) {
            consumer.accept(redisTrib);
        }
    }

    private <T> T computeRedisTrib(final Function<RedisTrib, T> function) {
        try (final RedisTrib redisTrib = new RedisTrib()) {
            return function.apply(redisTrib);
        }
    }

    private ClusterNode getClusterNode(final String hostAndPort) {
        final List<ClusterNode> nodes = getClusterNodes(hostAndPort);
        return nodes.stream()
                    .filter(v -> hostAndPort.equals(v.getHostAndPort()))
                    .findFirst()
                    .get();
    }

    private List<ClusterNode> getClusterNodes(final String hostAndPort) {
        return redisSupport.computeCommands(RedisURI.create(RedisSupport.REDIS_SCHEME + hostAndPort),
                                            commands -> redisSupport.parseClusterNodesResult(
                                                    commands.clusterNodes(), hostAndPort));
    }

    @Test
    public void getCreateClusterParams() {
        List<CreateClusterParam> result;

        // 1 replicas, 6 nodes
        result = computeRedisTrib(redisTrib -> redisTrib.getCreateClusterParams(
                1,
                testRedisProperties.getClusterUris()
                                   .stream()
                                   .map(RedisUri::toHostAndPort)
                                   .collect(Collectors.toList())));
        log.info("result={}", result);
        assertThat(result.get(0).getStartSlotNumber()).isEqualTo("0");
        assertThat(result.get(0).getEndSlotNumber()).isEqualTo("5460");
        assertThat(result.get(0).getMaster()).isEqualTo("127.0.0.1:10000");
        assertThat(result.get(0).getReplicas()).containsExactly("127.0.0.1:10003");
        assertThat(result.get(1).getStartSlotNumber()).isEqualTo("5461");
        assertThat(result.get(1).getEndSlotNumber()).isEqualTo("10921");
        assertThat(result.get(1).getMaster()).isEqualTo("127.0.0.1:10001");
        assertThat(result.get(1).getReplicas()).containsExactly("127.0.0.1:10004");
        assertThat(result.get(2).getStartSlotNumber()).isEqualTo("10922");
        assertThat(result.get(2).getEndSlotNumber()).isEqualTo("16383");
        assertThat(result.get(2).getMaster()).isEqualTo("127.0.0.1:10002");
        assertThat(result.get(2).getReplicas()).containsExactly("127.0.0.1:10005");

        // 0 replicas, 3 nodes
        result = computeRedisTrib(redisTrib -> redisTrib.getCreateClusterParams(
                0,
                testRedisProperties.getClusterUris()
                                   .stream()
                                   .map(RedisUri::toHostAndPort)
                                   .limit(3)
                                   .collect(Collectors.toList())));
        log.info("result={}", result);
        assertThat(result.get(0).getStartSlotNumber()).isEqualTo("0");
        assertThat(result.get(0).getEndSlotNumber()).isEqualTo("5460");
        assertThat(result.get(0).getMaster()).isEqualTo("127.0.0.1:10000");
        assertThat(result.get(0).getReplicas()).isEmpty();
        assertThat(result.get(1).getStartSlotNumber()).isEqualTo("5461");
        assertThat(result.get(1).getEndSlotNumber()).isEqualTo("10921");
        assertThat(result.get(1).getMaster()).isEqualTo("127.0.0.1:10001");
        assertThat(result.get(1).getReplicas()).isEmpty();
        assertThat(result.get(2).getStartSlotNumber()).isEqualTo("10922");
        assertThat(result.get(2).getEndSlotNumber()).isEqualTo("16383");
        assertThat(result.get(2).getMaster()).isEqualTo("127.0.0.1:10002");
        assertThat(result.get(2).getReplicas()).isEmpty();

        // 0 replicas, 4 nodes
        result = computeRedisTrib(redisTrib -> redisTrib.getCreateClusterParams(
                0,
                testRedisProperties.getClusterUris()
                                   .stream()
                                   .map(RedisUri::toHostAndPort)
                                   .limit(4)
                                   .collect(Collectors.toList())));
        log.info("result={}", result);
        assertThat(result.get(0).getStartSlotNumber()).isEqualTo("0");
        assertThat(result.get(0).getEndSlotNumber()).isEqualTo("4095");
        assertThat(result.get(0).getMaster()).isEqualTo("127.0.0.1:10000");
        assertThat(result.get(0).getReplicas()).isEmpty();
        assertThat(result.get(1).getStartSlotNumber()).isEqualTo("4096");
        assertThat(result.get(1).getEndSlotNumber()).isEqualTo("8191");
        assertThat(result.get(1).getMaster()).isEqualTo("127.0.0.1:10001");
        assertThat(result.get(1).getReplicas()).isEmpty();
        assertThat(result.get(2).getStartSlotNumber()).isEqualTo("8192");
        assertThat(result.get(2).getEndSlotNumber()).isEqualTo("12287");
        assertThat(result.get(2).getMaster()).isEqualTo("127.0.0.1:10002");
        assertThat(result.get(2).getReplicas()).isEmpty();
        assertThat(result.get(3).getStartSlotNumber()).isEqualTo("12288");
        assertThat(result.get(3).getEndSlotNumber()).isEqualTo("16383");
        assertThat(result.get(3).getMaster()).isEqualTo("127.0.0.1:10003");
        assertThat(result.get(3).getReplicas()).isEmpty();

        // 0 replicas, 5 nodes
        result = computeRedisTrib(redisTrib -> redisTrib.getCreateClusterParams(
                0,
                testRedisProperties.getClusterUris()
                                   .stream()
                                   .map(RedisUri::toHostAndPort)
                                   .limit(5)
                                   .collect(Collectors.toList())));
        log.info("result={}", result);
        assertThat(result.get(0).getStartSlotNumber()).isEqualTo("0");
        assertThat(result.get(0).getEndSlotNumber()).isEqualTo("3275");
        assertThat(result.get(0).getMaster()).isEqualTo("127.0.0.1:10000");
        assertThat(result.get(0).getReplicas()).isEmpty();
        assertThat(result.get(1).getStartSlotNumber()).isEqualTo("3276");
        assertThat(result.get(1).getEndSlotNumber()).isEqualTo("6551");
        assertThat(result.get(1).getMaster()).isEqualTo("127.0.0.1:10001");
        assertThat(result.get(1).getReplicas()).isEmpty();
        assertThat(result.get(2).getStartSlotNumber()).isEqualTo("6552");
        assertThat(result.get(2).getEndSlotNumber()).isEqualTo("9827");
        assertThat(result.get(2).getMaster()).isEqualTo("127.0.0.1:10002");
        assertThat(result.get(2).getReplicas()).isEmpty();
        assertThat(result.get(3).getStartSlotNumber()).isEqualTo("9828");
        assertThat(result.get(3).getEndSlotNumber()).isEqualTo("13103");
        assertThat(result.get(3).getMaster()).isEqualTo("127.0.0.1:10003");
        assertThat(result.get(3).getReplicas()).isEmpty();
        assertThat(result.get(4).getStartSlotNumber()).isEqualTo("13104");
        assertThat(result.get(4).getEndSlotNumber()).isEqualTo("16383");
        assertThat(result.get(4).getMaster()).isEqualTo("127.0.0.1:10004");
        assertThat(result.get(4).getReplicas()).isEmpty();

        // 0 replicas, 6 nodes
        result = computeRedisTrib(redisTrib -> redisTrib.getCreateClusterParams(
                0,
                testRedisProperties.getClusterUris()
                                   .stream()
                                   .map(RedisUri::toHostAndPort)
                                   .collect(Collectors.toList())));
        log.info("result={}", result);
        assertThat(result.get(0).getStartSlotNumber()).isEqualTo("0");
        assertThat(result.get(0).getEndSlotNumber()).isEqualTo("2729");
        assertThat(result.get(0).getMaster()).isEqualTo("127.0.0.1:10000");
        assertThat(result.get(0).getReplicas()).isEmpty();
        assertThat(result.get(1).getStartSlotNumber()).isEqualTo("2730");
        assertThat(result.get(1).getEndSlotNumber()).isEqualTo("5459");
        assertThat(result.get(1).getMaster()).isEqualTo("127.0.0.1:10001");
        assertThat(result.get(1).getReplicas()).isEmpty();
        assertThat(result.get(2).getStartSlotNumber()).isEqualTo("5460");
        assertThat(result.get(2).getEndSlotNumber()).isEqualTo("8189");
        assertThat(result.get(2).getMaster()).isEqualTo("127.0.0.1:10002");
        assertThat(result.get(2).getReplicas()).isEmpty();
        assertThat(result.get(3).getStartSlotNumber()).isEqualTo("8190");
        assertThat(result.get(3).getEndSlotNumber()).isEqualTo("10919");
        assertThat(result.get(3).getMaster()).isEqualTo("127.0.0.1:10003");
        assertThat(result.get(3).getReplicas()).isEmpty();
        assertThat(result.get(4).getStartSlotNumber()).isEqualTo("10920");
        assertThat(result.get(4).getEndSlotNumber()).isEqualTo("13649");
        assertThat(result.get(4).getMaster()).isEqualTo("127.0.0.1:10004");
        assertThat(result.get(4).getReplicas()).isEmpty();
        assertThat(result.get(5).getStartSlotNumber()).isEqualTo("13650");
        assertThat(result.get(5).getEndSlotNumber()).isEqualTo("16383");
        assertThat(result.get(5).getMaster()).isEqualTo("127.0.0.1:10005");
        assertThat(result.get(5).getReplicas()).isEmpty();
    }

    @Test
    public void getCreateClusterParamsError() throws Exception {
        // invalid replicas.
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.getCreateClusterParams(
                                -1,
                                testRedisProperties.getClusterUris()
                                                   .stream()
                                                   .map(RedisUri::toHostAndPort)
                                                   .collect(Collectors.toList()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Replicas must be equal or longer than 0. (-1)");

        // 0 replicas, 2 nodes
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.getCreateClusterParams(
                                0,
                                testRedisProperties.getClusterUris()
                                                   .stream()
                                                   .map(RedisUri::toHostAndPort)
                                                   .limit(2)
                                                   .collect(Collectors.toList()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Redis Cluster requires at least 3 master nodes. This is not possible with 2 nodes and 0 replicas per node. At least 3 nodes are required.");

        // 1 replicas, 3 nodes
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.getCreateClusterParams(
                                1,
                                testRedisProperties.getClusterUris()
                                                   .stream()
                                                   .map(RedisUri::toHostAndPort)
                                                   .limit(3)
                                                   .collect(Collectors.toList()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Redis Cluster requires at least 3 master nodes. This is not possible with 3 nodes and 1 replicas per node. At least 6 nodes are required.");

        // 1 replicas, 4 nodes
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.getCreateClusterParams(
                                1,
                                testRedisProperties.getClusterUris()
                                                   .stream()
                                                   .map(RedisUri::toHostAndPort)
                                                   .limit(4)
                                                   .collect(Collectors.toList()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Redis Cluster requires at least 3 master nodes. This is not possible with 4 nodes and 1 replicas per node. At least 6 nodes are required.");

        // 1 replicas, 5 nodes
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.getCreateClusterParams(
                                1,
                                testRedisProperties.getClusterUris()
                                                   .stream()
                                                   .map(RedisUri::toHostAndPort)
                                                   .limit(5)
                                                   .collect(Collectors.toList()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Redis Cluster requires at least 3 master nodes. This is not possible with 5 nodes and 1 replicas per node. At least 6 nodes are required.");

        // not cluster mode
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.getCreateClusterParams(
                                0,
                                testRedisProperties.getStandaloneUris()
                                                   .stream()
                                                   .map(RedisUri::toHostAndPort)
                                                   .collect(Collectors.toList()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "'127.0.0.1:10010' is not configured as a cluster node.");

        // not connected redis
        List<String> hasNotConnectedRedis = testRedisProperties.getClusterUris()
                                                               .stream()
                                                               .map(RedisUri::toHostAndPort)
                                                               .collect(Collectors.toList());
        hasNotConnectedRedis.add("127.0.0.1:10020");
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.getCreateClusterParams(0, hasNotConnectedRedis)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Failed to connect to node(127.0.0.1:10020).");

        // invalid host and port
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.getCreateClusterParams(
                                0, Lists.newArrayList("aaa", "bbb", "ccc"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "HostAndPort is invalid format. (aaa)");

        // invalid port
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.getCreateClusterParams(
                                0, Lists.newArrayList("aaa:aaa", "bbb:bbb", "ccc:ccc"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Port is invalid. (aaa)");

        // node already knows other nodes
        List<CreateClusterParam> params = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000", Lists.newArrayList("127.0.0.1:10003"),
                                       null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"),
                                       null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList("127.0.0.1:10005"),
                                       null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params));
        Thread.sleep(5000);
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.getCreateClusterParams(
                                0,
                                testRedisProperties.getClusterUris()
                                                   .stream()
                                                   .map(RedisUri::toHostAndPort)
                                                   .collect(Collectors.toList()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "'127.0.0.1:10000' is not empty. Either the node already knows other nodes (check with CLUSTER NODES) or contains some key in database 0.");
    }

    @Test
    public void createCluster() throws Exception {
        List<ClusterNode> result;

        // 1 replicas, 6 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList("127.0.0.1:10005"), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);
        result = getClusterNodes("127.0.0.1:10000");
        result.sort(Comparator.comparing(ClusterNode::getHostAndPort));
        log.info("result={}", result);
        assertThat(result.get(0).getHostAndPort()).isEqualTo("127.0.0.1:10000");
        assertThat(result.get(0).hasFlag("master")).isTrue();
        assertThat(result.get(0).getServedSlots()).isEqualTo("0-5460");
        assertThat(result.get(1).getHostAndPort()).isEqualTo("127.0.0.1:10001");
        assertThat(result.get(1).hasFlag("master")).isTrue();
        assertThat(result.get(1).getServedSlots()).isEqualTo("5461-10921");
        assertThat(result.get(2).getHostAndPort()).isEqualTo("127.0.0.1:10002");
        assertThat(result.get(2).hasFlag("master")).isTrue();
        assertThat(result.get(2).getServedSlots()).isEqualTo("10922-16383");
        assertThat(result.get(3).getHostAndPort()).isEqualTo("127.0.0.1:10003");
        assertThat(result.get(3).hasFlag("slave")).isTrue();
        assertThat(result.get(4).getHostAndPort()).isEqualTo("127.0.0.1:10004");
        assertThat(result.get(4).hasFlag("slave")).isTrue();
        assertThat(result.get(5).getHostAndPort()).isEqualTo("127.0.0.1:10005");
        assertThat(result.get(5).hasFlag("slave")).isTrue();

        // 0 replicas, 6 nodes
        resetAllRedis();
        List<CreateClusterParam> params2 = Lists.newArrayList(
                new CreateClusterParam("0", "1000", "127.0.0.1:10000",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("1001", "2000", "127.0.0.1:10001",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("2001", "3000", "127.0.0.1:10002",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("3001", "4000", "127.0.0.1:10003",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("4001", "5000", "127.0.0.1:10004",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("5001", "16383", "127.0.0.1:10005",
                                       Lists.newArrayList(), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params2));
        Thread.sleep(5000);
        result = getClusterNodes("127.0.0.1:10000");
        result.sort(Comparator.comparing(ClusterNode::getHostAndPort));
        log.info("result={}", result);
        assertThat(result.get(0).getHostAndPort()).isEqualTo("127.0.0.1:10000");
        assertThat(result.get(0).hasFlag("master")).isTrue();
        assertThat(result.get(0).getServedSlots()).isEqualTo("0-1000");
        assertThat(result.get(1).getHostAndPort()).isEqualTo("127.0.0.1:10001");
        assertThat(result.get(1).hasFlag("master")).isTrue();
        assertThat(result.get(1).getServedSlots()).isEqualTo("1001-2000");
        assertThat(result.get(2).getHostAndPort()).isEqualTo("127.0.0.1:10002");
        assertThat(result.get(2).hasFlag("master")).isTrue();
        assertThat(result.get(2).getServedSlots()).isEqualTo("2001-3000");
        assertThat(result.get(3).getHostAndPort()).isEqualTo("127.0.0.1:10003");
        assertThat(result.get(3).hasFlag("master")).isTrue();
        assertThat(result.get(3).getServedSlots()).isEqualTo("3001-4000");
        assertThat(result.get(4).getHostAndPort()).isEqualTo("127.0.0.1:10004");
        assertThat(result.get(4).hasFlag("master")).isTrue();
        assertThat(result.get(4).getServedSlots()).isEqualTo("4001-5000");
        assertThat(result.get(5).getHostAndPort()).isEqualTo("127.0.0.1:10005");
        assertThat(result.get(5).hasFlag("master")).isTrue();
        assertThat(result.get(5).getServedSlots()).isEqualTo("5001-16383");

        // 1 replicas, 5 nodes (a node does not have slave)
        resetAllRedis();
        List<CreateClusterParam> params3 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList(), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params3));
        Thread.sleep(5000);
        result = getClusterNodes("127.0.0.1:10000");
        result.sort(Comparator.comparing(ClusterNode::getHostAndPort));
        log.info("result={}", result);
        assertThat(result.get(0).getHostAndPort()).isEqualTo("127.0.0.1:10000");
        assertThat(result.get(0).hasFlag("master")).isTrue();
        assertThat(result.get(0).getServedSlots()).isEqualTo("0-5460");
        assertThat(result.get(1).getHostAndPort()).isEqualTo("127.0.0.1:10001");
        assertThat(result.get(1).hasFlag("master")).isTrue();
        assertThat(result.get(1).getServedSlots()).isEqualTo("5461-10921");
        assertThat(result.get(2).getHostAndPort()).isEqualTo("127.0.0.1:10002");
        assertThat(result.get(2).hasFlag("master")).isTrue();
        assertThat(result.get(2).getServedSlots()).isEqualTo("10922-16383");
        assertThat(result.get(3).getHostAndPort()).isEqualTo("127.0.0.1:10003");
        assertThat(result.get(3).hasFlag("slave")).isTrue();
        assertThat(result.get(4).getHostAndPort()).isEqualTo("127.0.0.1:10004");
        assertThat(result.get(4).hasFlag("slave")).isTrue();
        assertThat(result.size()).isEqualTo(5);
    }

    @Test
    public void createClusterError() {
        // 0-999 slot is not set.
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("1000", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList("127.0.0.1:10005"), null)
        );
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.createCluster(params1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Slot is not enough. You must specify 0-999.");

        // Duplicate host and port.
        List<CreateClusterParam> params2 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10005"), null)
        );
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.createCluster(params2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate hostAndPort(127.0.0.1:10000).");
    }

    @Test
    public void checkCluster() throws Exception {
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList("127.0.0.1:10005"), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);
        executeRedisTrib(redisTrib -> {
            List<String> result = redisTrib.checkCluster("127.0.0.1:10000");
            assertThat(result).isEmpty();
        });

        // break slot coverage
        redisSupport.executeCommands(testRedisProperties.getClusterUris().get(0).toRedisURI(), commands -> {
            commands.clusterDelSlots(0, 1, 2); //delete
        });
        Thread.sleep(5000);
        executeRedisTrib(redisTrib -> {
            List<String> result = redisTrib.checkCluster("127.0.0.1:10000");
            // error added
            assertThat(result).isNotEmpty();
        });
    }

    @Test
    public void reshardCluster() throws Exception {
        // 1 replicas, 6 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList("127.0.0.1:10005"), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        // ALL pattern
        ClusterNode node = getClusterNode("127.0.0.1:10000");
        executeRedisTrib(redisTrib -> {
            redisTrib.reshardCluster("127.0.0.1:10000", 1000, "ALL", node.getNodeId());
        });
        Thread.sleep(5000);
        ClusterNode nodeAfter1 = getClusterNode("127.0.0.1:10000");
        assertThat(nodeAfter1.getServedSlots()).isEqualTo("0-5959,10922-11422");

        // specify  pattern
        ClusterNode fromNode = getClusterNode("127.0.0.1:10001");
        executeRedisTrib(redisTrib -> {
            redisTrib.reshardCluster("127.0.0.1:10000", 1000, fromNode.getNodeId(), node.getNodeId());
        });
        Thread.sleep(5000);
        ClusterNode nodeAfter2 = getClusterNode("127.0.0.1:10000");
        assertThat(nodeAfter2.getServedSlots()).isEqualTo("0-6959,10922-11422");
    }

    @Test
    public void reshardClusterError() throws Exception {
        // 1 replicas, 6 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList("127.0.0.1:10005"), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        List<ClusterNode> nodes = getClusterNodes("127.0.0.1:10000");
        List<ClusterNode> masterNodes = nodes.stream()
                                             .filter(v -> v.hasFlag("master"))
                                             .collect(Collectors.toList());
        List<ClusterNode> slaveNodes = nodes.stream()
                                            .filter(v -> v.hasFlag("slave"))
                                            .collect(Collectors.toList());

        // toNodeId is invalid
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.reshardCluster("127.0.0.1:10000", 10, "ALL", "invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The specified node is not known or not a master.");

        // toNodeId is slave
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib
                                .reshardCluster("127.0.0.1:10000", 10, "ALL", slaveNodes.get(0).getNodeId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The specified node is not known or not a master.");

        // fromNodeIds is invalid
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib
                                .reshardCluster("127.0.0.1:10000", 10, "invalid",
                                                masterNodes.get(0).getNodeId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The specified node is not known or not a master.");

        // fromNodeIds contain slave
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib
                                .reshardCluster("127.0.0.1:10000", 10, slaveNodes.get(0).getNodeId(),
                                                masterNodes.get(0).getNodeId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The specified node is not known or not a master.");

        // fromNodeIds contain toNodeId
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib
                                .reshardCluster("127.0.0.1:10000", 10, masterNodes.get(0).getNodeId(),
                                                masterNodes.get(0).getNodeId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Target node is also listed among the source nodes.");
    }

    @Test
    public void reshardClusterBySlots() throws Exception {
        // 1 replicas, 6 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList("127.0.0.1:10005"), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        // ALL pattern
        ClusterNode node = getClusterNode("127.0.0.1:10000");
        Set<Integer> slots = IntStream.range(10000, 11000).boxed().collect(Collectors.toSet());
        executeRedisTrib(redisTrib -> {
            redisTrib.reshardClusterBySlots("127.0.0.1:10000", slots, node.getNodeId());
        });
        Thread.sleep(5000);
        ClusterNode nodeAfter1 = getClusterNode("127.0.0.1:10000");
        assertThat(nodeAfter1.getServedSlots()).isEqualTo("0-5460,10000-10999");
    }

    @Test
    public void addNodeIntoCluster() throws Exception {
        // 0 replicas, 3 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList(), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        // Add node as master
        executeRedisTrib(redisTrib -> {
            redisTrib.addNodeIntoCluster("127.0.0.1:10000", "127.0.0.1:10003");
        });
        ClusterNode node = getClusterNode("127.0.0.1:10003");
        assertThat(node.getHostAndPort()).isEqualTo("127.0.0.1:10003");
        assertThat(node.hasFlag("master")).isTrue();
        assertThat(node.getServedSlotsSet()).isEmpty();
    }

    @Test
    public void addNodeIntoClusterError() throws Exception {
        // 0 replicas, 3 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList(), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        // invalid hostAndPort
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.addNodeIntoCluster("invalid", "127.0.0.1:10003")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("HostAndPort is invalid format. (invalid)");

        // invalid newHostAndPort
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.addNodeIntoCluster("127.0.0.1:10000", "invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("HostAndPort is invalid format. (invalid)");

        // not empty
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.addNodeIntoCluster("127.0.0.1:10000", "127.0.0.1:10000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "'127.0.0.1:10000' is not empty. Either the node already knows other nodes (check with CLUSTER NODES) or contains some key in database 0.");
    }

    @Test
    public void addNodeIntoClusterAsReplicaRandom() throws Exception {
        // 1 replicas, 5 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList(), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        // Add node as replica, master node is random
        executeRedisTrib(redisTrib -> {
            redisTrib.addNodeIntoClusterAsReplica("127.0.0.1:10000", "127.0.0.1:10005", "random");
        });
        ClusterNode node = getClusterNode("127.0.0.1:10005");
        ClusterNode masterNode = getClusterNode("127.0.0.1:10002");
        assertThat(node.getHostAndPort()).isEqualTo("127.0.0.1:10005");
        assertThat(node.hasFlag("slave")).isTrue();
        assertThat(node.getMasterNodeId()).isEqualTo(masterNode.getNodeId());
    }

    @Test
    public void addNodeIntoClusterAsReplicaWithMasterNodeId() throws Exception {
        // 1 replicas, 5 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList(), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        // Add node as replica
        ClusterNode masterNode = getClusterNode("127.0.0.1:10002");
        executeRedisTrib(redisTrib -> {
            redisTrib.addNodeIntoClusterAsReplica("127.0.0.1:10000", "127.0.0.1:10003", masterNode.getNodeId());
        });
        ClusterNode node = getClusterNode("127.0.0.1:10003");
        assertThat(node.getHostAndPort()).isEqualTo("127.0.0.1:10003");
        assertThat(node.hasFlag("slave")).isTrue();
        assertThat(node.getMasterNodeId()).isEqualTo(masterNode.getNodeId());
    }

    @Test
    public void addNodeIntoClusterAsReplicaError() throws Exception {
        // 1 replicas, 5 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList(), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList(), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        // invalid maserId
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib
                                .addNodeIntoClusterAsReplica("127.0.0.1:10000", "127.0.0.1:10004", "invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No such master ID. (invalid)");

        // specify slave nodeId
        ClusterNode slaveNode = getClusterNode("127.0.0.1:10003");
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib
                                .addNodeIntoClusterAsReplica("127.0.0.1:10000", "127.0.0.1:10004",
                                                             slaveNode.getNodeId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("This node is not master node.");

        // not empty
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib
                                .addNodeIntoClusterAsReplica("127.0.0.1:10000", "127.0.0.1:10001", "random")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "'127.0.0.1:10001' is not empty. Either the node already knows other nodes (check with CLUSTER NODES) or contains some key in database 0.");
    }

    @Test
    public void deleteNodeOfCluster() throws Exception {
        // 1 replicas, 5 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList("127.0.0.1:10005"), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        // not reset
        ClusterNode slave1 = getClusterNode("127.0.0.1:10003");
        executeRedisTrib(
                redisTrib -> redisTrib.deleteNodeOfCluster("127.0.0.1:10000", slave1.getNodeId(), null, false));
        List<ClusterNode> slave1Nodes = getClusterNodes("127.0.0.1:10003");
        assertThat(slave1Nodes.size()).isEqualTo(6);

        // soft reset
        ClusterNode slave2 = getClusterNode("127.0.0.1:10004");
        executeRedisTrib(
                redisTrib -> redisTrib
                        .deleteNodeOfCluster("127.0.0.1:10000", slave2.getNodeId(), "soft", false));
        List<ClusterNode> slave2Nodes = getClusterNodes("127.0.0.1:10004");
        assertThat(slave2Nodes.size()).isEqualTo(1);
        assertThat(slave2Nodes.get(0).getNodeId()).isEqualTo(slave2.getNodeId());

        // soft reset
        ClusterNode slave3 = getClusterNode("127.0.0.1:10005");
        executeRedisTrib(
                redisTrib -> redisTrib
                        .deleteNodeOfCluster("127.0.0.1:10000", slave3.getNodeId(), "hard", false));
        List<ClusterNode> slave3Nodes = getClusterNodes("127.0.0.1:10005");
        assertThat(slave3Nodes.size()).isEqualTo(1);
        assertThat(slave3Nodes.get(0).getNodeId()).isNotEqualTo(slave3.getNodeId());
    }

    @Test
    public void deleteNodeOfClusterError() throws Exception {
        // 1 replicas, 5 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList("127.0.0.1:10005"), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        // not exist nodeId
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.deleteNodeOfCluster("127.0.0.1:10000", "invalid", null, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No such node ID. (invalid)");

        // master
        ClusterNode master = getClusterNode("127.0.0.1:10001");
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib
                                .deleteNodeOfCluster("127.0.0.1:10000", master.getNodeId(), null, false)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Node(127.0.0.1:10001) is not empty! Reshard data away and try again.");
    }

    @Test
    public void replicateNode() throws Exception {
        // 0 replicas, 5 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList(), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);
        // add empty master
        executeRedisTrib(redisTrib -> redisTrib.addNodeIntoCluster("127.0.0.1:10000", "127.0.0.1:10005"));

        // change master
        ClusterNode master1 = getClusterNode("127.0.0.1:10000");
        executeRedisTrib(redisTrib -> redisTrib.replicateNode("127.0.0.1:10004", master1.getNodeId()));
        ClusterNode slave2 = getClusterNode("127.0.0.1:10004");
        assertThat(slave2.getMasterNodeId()).isEqualTo(master1.getNodeId());

        // master to slave
        executeRedisTrib(redisTrib -> redisTrib.replicateNode("127.0.0.1:10005", master1.getNodeId()));
        ClusterNode slave3 = getClusterNode("127.0.0.1:10005");
        assertThat(slave3.getMasterNodeId()).isEqualTo(master1.getNodeId());
    }

    @Test
    public void replicateNodeError() throws Exception {
        // 0 replicas, 5 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList(), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        // this is master
        ClusterNode master1 = getClusterNode("127.0.0.1:10000");
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.replicateNode("127.0.0.1:10001", master1.getNodeId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Node(127.0.0.1:10001) is not empty! Reshard data away and try again.");

        // invalid master node id
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.replicateNode("127.0.0.1:10003", "invalid")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Node(invalid) does not exists.");

        // target is not master
        ClusterNode slave2 = getClusterNode("127.0.0.1:10004");
        assertThatThrownBy(
                () -> executeRedisTrib(
                        redisTrib -> redisTrib.replicateNode("127.0.0.1:10003", slave2.getNodeId())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format("Node(%s) is not master.", slave2.getNodeId()));
    }

    @Test
    public void failoverNode() throws Exception {
        // 0 replicas, 5 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList("127.0.0.1:10005"), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        executeRedisTrib(redisTrib -> redisTrib.failoverNode("127.0.0.1:10003"));
        ClusterNode master1 = getClusterNode("127.0.0.1:10000");
        assertThat(master1.hasFlag("slave")).isTrue();
    }

    @Test
    public void failoverError() throws Exception {
        // 0 replicas, 5 nodes
        List<CreateClusterParam> params1 = Lists.newArrayList(
                new CreateClusterParam("0", "5460", "127.0.0.1:10000",
                                       Lists.newArrayList("127.0.0.1:10003"), null),
                new CreateClusterParam("5461", "10921", "127.0.0.1:10001",
                                       Lists.newArrayList("127.0.0.1:10004"), null),
                new CreateClusterParam("10922", "16383", "127.0.0.1:10002",
                                       Lists.newArrayList("127.0.0.1:10005"), null)
        );
        executeRedisTrib(redisTrib -> redisTrib.createCluster(params1));
        Thread.sleep(5000);

        assertThatThrownBy(() -> executeRedisTrib(redisTrib -> redisTrib.failoverNode("127.0.0.1:10000")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Node(127.0.0.1:10000) is not slave.");
    }
}
