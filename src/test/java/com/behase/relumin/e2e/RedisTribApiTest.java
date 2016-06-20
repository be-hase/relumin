package com.behase.relumin.e2e;

import com.behase.relumin.Application;
import com.behase.relumin.TestHelper;
import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.support.JedisSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Check integration of success behavior
 */
@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest
public class RedisTribApiTest {
    @Value("${test.redis.host}")
    private String testRedisHost;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private JedisSupport jedisSupport;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = webAppContextSetup(wac).build();
        clear();
    }

    private void clear() {
        testHelper.resetAllRedis();
    }

    @Test
    public void test() throws Exception {
        Cluster cluster;
        /**
         * GET /api/trib/create/params
         * POST /api/trib/create/{clusterName}
         */
        {
            MvcResult result = mockMvc.perform(
                    get("/api/trib/create/params")
                            .param("replicas", "1")
                            .param("hostAndPorts", testRedisHost + ":10000-10005")
            ).andReturn();
            String body = result.getResponse().getContentAsString();
            log.debug("result={}", body);

            result = mockMvc.perform(
                    post("/api/trib/create/test1")
                            .param("params", body)
                            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cluster_name", is("test1")))
                    .andExpect(jsonPath("$.status", is("ok")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
            cluster = mapper.readValue(result.getResponse().getContentAsString(), Cluster.class);
        }

        /**
         * GET /api/trib/check
         */
        {
            MvcResult result = mockMvc.perform(
                    get("/api/trib/check")
                            .param("clusterName", "test1")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * POST /api/trib/reshard
         */
        {
            ClusterNode masterNode = getNode(cluster, testRedisHost + ":10000");

            MvcResult result = mockMvc.perform(
                    post("/api/trib/reshard")
                            .param("clusterName", "test1")
                            .param("slotCount", "50")
                            .param("fromNodeIds", "ALL")
                            .param("toNodeId", masterNode.getNodeId())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cluster_name", is(nullValue())))
                    .andExpect(jsonPath("$.nodes[0].slot_count", is(greaterThan(5500))))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
            cluster = mapper.readValue(result.getResponse().getContentAsString(), Cluster.class);
        }

        /**
         * POST /api/trib/reshard-by-slots
         */
        {
            ClusterNode masterNode = getNode(cluster, testRedisHost + ":10000");

            MvcResult result = mockMvc.perform(
                    post("/api/trib/reshard-by-slots")
                            .param("clusterName", "test1")
                            .param("slots", "16333-16383")
                            .param("toNodeId", masterNode.getNodeId())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cluster_name", is(nullValue())))
                    .andExpect(jsonPath("$.nodes[0].slot_count", is(greaterThan(5550))))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
            cluster = mapper.readValue(result.getResponse().getContentAsString(), Cluster.class);
        }

        /**
         * POST /api/trib/delete-node
         * POST /api/trib/add-node
         */
        {
            ClusterNode slaveNode = getNode(cluster, testRedisHost + ":10003");

            MvcResult result = mockMvc.perform(
                    post("/api/trib/delete-node")
                            .param("clusterName", "test1")
                            .param("nodeId", slaveNode.getNodeId())
                            .param("reset", "HARD")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.cluster_known_nodes", is("5")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
            cluster = mapper.readValue(result.getResponse().getContentAsString(), Cluster.class);

            result = mockMvc.perform(
                    post("/api/trib/add-node")
                            .param("clusterName", "test1")
                            .param("hostAndPort", slaveNode.getHostAndPort())
                            .param("masterNodeId", slaveNode.getMasterNodeId())
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.cluster_known_nodes", is("6")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
            cluster = mapper.readValue(result.getResponse().getContentAsString(), Cluster.class);
        }

        /**
         * POST /api/trib/replicate
         */
        {
            ClusterNode masterNode = getNode(cluster, testRedisHost + ":10000");
            ClusterNode slaveNode = getNode(cluster, testRedisHost + ":10004");

            MvcResult result = mockMvc.perform(
                    post("/api/trib/replicate")
                            .param("hostAndPort", slaveNode.getHostAndPort())
                            .param("masterNodeId", masterNode.getNodeId())
            )
                    .andExpect(status().isOk())
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
            cluster = mapper.readValue(result.getResponse().getContentAsString(), Cluster.class);

            long slaveCount = cluster.getNodes().stream()
                    .filter(v -> v.getMasterNodeId().equals(masterNode.getNodeId())).count();
            assertThat(slaveCount, is(2L));
        }

        /**
         * POST /api/trib/failover
         */
        {
            ClusterNode slaveNode = getNode(cluster, testRedisHost + ":10003");

            MvcResult result = mockMvc.perform(
                    post("/api/trib/failover")
                            .param("hostAndPort", slaveNode.getHostAndPort())
            )
                    .andExpect(status().isOk())
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
            cluster = mapper.readValue(result.getResponse().getContentAsString(), Cluster.class);
            ClusterNode preMasterNode = cluster.getNodes().stream()
                    .filter(v -> slaveNode.getMasterNodeId().equals(v.getNodeId())).findFirst().get();
            assertThat(preMasterNode.hasFlag("master"), is(false));
        }
    }

    @Test
    public void onlyCreate() throws Exception {
        /**
         * GET /api/create/params
         * POST /api/create/{clusterName}
         */
        {
            MvcResult result = mockMvc.perform(
                    get("/api/trib/create/params")
                            .param("replicas", "1")
                            .param("hostAndPorts", testRedisHost + ":10000-10005")
            ).andReturn();
            String body = result.getResponse().getContentAsString();
            log.debug("result={}", body);

            result = mockMvc.perform(
                    post("/api/trib/create")
                            .param("params", body)
                            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cluster_name", is(nullValue())))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }
    }

    private ClusterNode getNode(Cluster cluster, String hostAndPort) {
        return cluster.getNodes().stream().filter(v -> v.getHostAndPort().equals(hostAndPort)).findFirst().get();
    }
}
