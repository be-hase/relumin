package com.behase.relumin.e2e;

import com.behase.relumin.Application;
import com.behase.relumin.TestHelper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import redis.clients.jedis.JedisPool;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@ActiveProfiles("test")
@WebIntegrationTest
public class ClusterApiTest {
    @Value("${test.redis.host}")
    private String testRedisHost;

    @Autowired
    private JedisPool dataStoreJedisPool;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private WebApplicationContext wac;

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
        /**
         * /create/{clusterName}
         */
        {
            // given
            MvcResult result = mockMvc.perform(
                    get("/api/trib/create/params")
                            .param("replicas", "1")
                            .param("hostAndPorts", testRedisHost + ":10000-10005")
            ).andReturn();
            String body = result.getResponse().getContentAsString();

            // when, then
            result = mockMvc.perform(
                    post("/api/trib/create/test")
                            .param("params", body)
                            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cluster_name", is("test")))
                    .andExpect(jsonPath("$.status", is("ok")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * /create/{clusterName}
         */
        {
            MvcResult result = mockMvc.perform(
                    get("/api/trib/create/test")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cluster_name", is("test")))
                    .andExpect(jsonPath("$.status", is("ok")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }
    }
}
