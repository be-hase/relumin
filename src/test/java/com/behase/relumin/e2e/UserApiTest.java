package com.behase.relumin.e2e;

import com.behase.relumin.Application;
import com.behase.relumin.TestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
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
public class UserApiTest {
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
         * POST /api/user/{username}
         */
        {
            MvcResult result = mockMvc.perform(
                    post("/api/user/user1")
                            .param("displayName", "display name 1")
                            .param("role", "RELUMIN_ADMIN")
                            .param("password", "hogehoge")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username", is("user1")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * GET /api/users
         */
        {
            MvcResult result = mockMvc.perform(
                    get("/api/users")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].username", is("user1")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * POST /api/user/{username}
         */
        {
            MvcResult result = mockMvc.perform(
                    post("/api/user/user1/update")
                            .param("displayName", "changed")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.display_name", is("changed")))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }

        /**
         * POST /api/user/{username}/delete
         */
        {
            MvcResult result = mockMvc.perform(
                    post("/api/user/user1/delete")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess", is(true)))
                    .andReturn();
            log.debug("result={}", result.getResponse().getContentAsString());
        }
    }
}
