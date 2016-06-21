package com.behase.relumin;

import com.behase.relumin.model.Cluster;
import com.behase.relumin.support.JedisSupport;
import com.behase.relumin.webconfig.WebConfig;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Slf4j
@Component
public class TestHelper {
    @Value("${test.redis.host}")
    private String testRedisHost;

    public String getDataStoreJedis() {
        return testRedisHost + ":9000";
    }

    public Set<String> getRedisClusterHostAndPorts() {
        return new JedisSupport().getHostAndPorts(Lists.newArrayList(testRedisHost + ":10000-10005"));
    }

    public Set<String> getRedisStandAloneHostAndPorts() {
        return new JedisSupport().getHostAndPorts(Lists.newArrayList(testRedisHost + ":10010-10015"));
    }

    public void resetAllRedis() {
        log.info("reset all redis.");

        try (Jedis jedis = new JedisSupport().getJedisByHostAndPort(getDataStoreJedis())) {
            try {
                jedis.flushAll();
            } catch (Exception e) {
            }
        }

        for (String hostAndPort : getRedisClusterHostAndPorts()) {
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

        for (String hostAndPort : getRedisStandAloneHostAndPorts()) {
            try (Jedis jedis = new JedisSupport().getJedisByHostAndPort(hostAndPort)) {
                jedis.flushAll();
            } catch (Exception e) {
            }
        }
    }

    public void createBasicCluster(MockMvc mockMvc, String clusterName) throws Exception {
        MvcResult result;

        result = mockMvc.perform(
                get("/api/trib/create/params")
                        .param("replicas", "1")
                        .param("hostAndPorts", testRedisHost + ":10000-10005")
        ).andReturn();
        String body = result.getResponse().getContentAsString();

        mockMvc.perform(
                post("/api/trib/create/" + clusterName)
                        .param("params", body)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        );
    }

    public void createOnlyMasterCluster(MockMvc mockMvc, String clusterName) throws Exception {
        MvcResult result;

        result = mockMvc.perform(
                get("/api/trib/create/params")
                        .param("replicas", "0")
                        .param("hostAndPorts", testRedisHost + ":10000-10002")
        ).andReturn();
        String body = result.getResponse().getContentAsString();

        mockMvc.perform(
                post("/api/trib/create/" + clusterName)
                        .param("params", body)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        );
    }

    public Cluster getCluster(MockMvc mockMvc, String clusterName) throws Exception {
        MvcResult result = mockMvc.perform(
                get("/api/cluster/" + clusterName)
        ).andReturn();
        return WebConfig.MAPPER.readValue(result.getResponse().getContentAsString(), Cluster.class);
    }
}
