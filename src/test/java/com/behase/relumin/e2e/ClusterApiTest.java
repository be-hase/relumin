package com.behase.relumin.e2e;

import com.behase.relumin.Application;
import com.behase.relumin.TestHelper;
import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.Notice;
import com.behase.relumin.scheduler.NodeScheduler;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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
public class ClusterApiTest {
    @Value("${test.redis.host}")
    private String testRedisHost;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private NodeScheduler nodeScheduler;

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

        // given
        {
            MvcResult result = mockMvc.perform(
                    get("/api/trib/create/params")
                            .param("replicas", "1")
                            .param("hostAndPorts", testRedisHost + ":10000-10005")
            ).andReturn();
            String body = result.getResponse().getContentAsString();

            result = mockMvc.perform(
                    post("/api/trib/create/test1")
                            .param("params", body)
                            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cluster_name", is("test1")))
                    .andExpect(jsonPath("$.status", is("ok")))
                    .andReturn();
            cluster = mapper.readValue(result.getResponse().getContentAsString(), Cluster.class);
        }

        /**
         * GET /api/cluster/{clusterName}
         */
        {
            MvcResult result = mockMvc.perform(
                    get("/api/cluster/test1")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cluster_name", is("test1")))
                    .andExpect(jsonPath("$.status", is("ok")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * POST /api/cluster/{clusterName}
         */
        {
            MvcResult result = mockMvc.perform(
                    post("/api/cluster/test2").param("hostAndPort", testRedisHost + ":10000")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cluster_name", is("test2")))
                    .andExpect(jsonPath("$.status", is("ok")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * GET /api/clusters
         */
        {
            MvcResult result = mockMvc.perform(
                    get("/api/clusters")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0]", is("test1")))
                    .andExpect(jsonPath("$[1]", is("test2")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * GET /api/clusters?full=true
         */
        {
            MvcResult result = mockMvc.perform(
                    get("/api/clusters?full=true")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].cluster_name", is("test1")))
                    .andExpect(jsonPath("$[1].cluster_name", is("test2")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * POST /api/cluster/{clusterName}/change-cluster-name
         */
        {
            MvcResult result = mockMvc.perform(
                    post("/api/cluster/test2/change-cluster-name")
                            .param("newClusterName", "test2new")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cluster_name", is("test2new")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * POST /api/cluster/{clusterName}/delete
         */
        {
            MvcResult result = mockMvc.perform(
                    post("/api/cluster/test2new/delete")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess", is(true)))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * POST /api/cluster/{clusterName}/notice
         */
        {
            Notice notice = new Notice();
            notice.setInvalidEndTime("2016-06-20 00:00:00");
            MvcResult result = mockMvc.perform(
                    post("/api/cluster/test1/notice")
                            .param("notice", mapper.writeValueAsString(notice))
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.invalid_end_time", is("2016-06-20 00:00:00")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * GET /api/cluster/{clusterName}/notice
         */
        {
            MvcResult result = mockMvc.perform(
                    get("/api/cluster/test1/notice")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.invalid_end_time", is("2016-06-20 00:00:00")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * POST /api/cluster/{clusterName}/metrics
         */
        {
            // collect
            nodeScheduler.collectStaticsInfo();
            String nodeId = cluster.getNodes().get(0).getNodeId();

            MvcResult result = mockMvc.perform(
                    post("/api/cluster/test1/metrics")
                            .param("nodes", nodeId)
                            .param("fields", "used_memory")
                            .param("start", System.currentTimeMillis() - 10000 + "")
                            .param("end", System.currentTimeMillis() + "")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$." + nodeId + ".used_memory").isArray())
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * GET /api/cluster/{clusterName}/slowlogs
         */
        {
            MvcResult result = mockMvc.perform(
                    get("/api/cluster/test1/slowlogs")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.offset", is(0)))
                    .andExpect(jsonPath("$.limit", is(1000)))
                    .andExpect(jsonPath("$.current_page", is(1)))
                    .andExpect(jsonPath("$.last_page", is(1)))
                    .andExpect(jsonPath("$.prev_page", is(nullValue())))
                    .andExpect(jsonPath("$.next_page", is(nullValue())))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }
    }
}
