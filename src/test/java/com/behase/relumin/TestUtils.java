package com.behase.relumin;

import com.behase.relumin.support.JedisSupport;
import com.google.common.collect.Lists;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.List;
import java.util.Set;

public class TestUtils {
    public static Set<String> getRedisClusterHostAndPorts(String testRedisHost) {
        return new JedisSupport().getHostAndPorts(Lists.newArrayList(testRedisHost + ":10000-10005"));
    }

//    public static String getRedisClusterHostAndPort(String testRedisHost, int index) {
//        List<String> list = Lists.newArrayList(getRedisClusterHostAndPorts(testRedisHost));
//        return list.get(index);
//    }

    public static Set<String> getRedisStandAloneHostAndPorts(String testRedisHost) {
        return new JedisSupport().getHostAndPorts(Lists.newArrayList(testRedisHost + ":10010-10015"));
    }

//    public static String getRedisStandAloneHostAndPort(String testRedisHost, int index) {
//        List<String> list = Lists.newArrayList(getRedisStandAloneHostAndPorts(testRedisHost));
//        return list.get(index);
//    }

    public static void resetAllRedis(String testRedisHost) {
        // empty (and reset)
        for (String hostAndPort : getRedisClusterHostAndPorts(testRedisHost)) {
            try (Jedis jedis = new JedisSupport().getJedisByHostAndPort(hostAndPort)) {
                try {
                    jedis.flushAll();
                } catch (Exception e) {
                }
                try {
                    jedis.clusterReset(JedisCluster.Reset.HARD);
                } catch (Exception e) {
                }
            } catch (Exception e) {
            }
        }

        for (String hostAndPort : getRedisStandAloneHostAndPorts(testRedisHost)) {
            try (Jedis jedis = new JedisSupport().getJedisByHostAndPort(hostAndPort)) {
                jedis.flushAll();
            } catch (Exception e) {
            }
        }
    }
}
