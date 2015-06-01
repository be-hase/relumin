package com.behase.relumin;

import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import org.apache.commons.lang3.StringUtils;
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

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster.Reset;
import redis.clients.jedis.JedisPool;

import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.util.JedisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@ActiveProfiles("test")
@WebIntegrationTest
public class ApiTest {
	@Value("${test.redis.normalCluster}")
	private String testRedisNormalCluster;

	@Value("${test.redis.emptyCluster}")
	private String testRedisEmptyCluster;

	@Value("${test.redis.emptyClusterAll}")
	private String testRedisEmptyClusterAll;

	@Value("${test.redis.normalStandAlone}")
	private String testRedisNormalStandAlone;

	@Value("${test.redis.normalStandAloneAll}")
	private String testRedisNormalStandAloneAll;

	@Value("${test.redis.emptyStandAlone}")
	private String testRedisEmptyStandAlone;

	@Value("${test.redis.emptyStandAloneAll}")
	private String testRedisEmptyStandAloneAll;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	JedisPool dataStoreJedisPool;

	@Autowired
	ObjectMapper mapper;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		mockMvc = webAppContextSetup(wac).build();
		clear();
	}

	private void clear() {
		try (Jedis jedis = dataStoreJedisPool.getResource()) {
			jedis.flushAll();
		} catch (Exception e) {
		}

		// empty (and reset)
		for (String item : StringUtils.split(testRedisEmptyClusterAll, ",")) {
			try (Jedis jedis = JedisUtils.getJedisByHostAndPort(item)) {
				try {
					jedis.flushAll();
				} catch (Exception e) {
				}
				try {
					jedis.clusterReset(Reset.HARD);
				} catch (Exception e) {
				}
			} catch (Exception e) {
			}
		}
	}

	private void createBasicCluster() throws Exception {
		MvcResult result;

		result = mockMvc.perform(
			get("/api/trib/create/params")
				.param("replicas", "1")
				.param("hostAndPorts", "192.168.33.11:8000-8005")
			)
			.andReturn();
		String body = result.getResponse().getContentAsString();

		result = mockMvc.perform(
			post("/api/trib/create/test")
				.param("params", body)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
			)
			.andReturn();
	}

	private void createOnlyMasterCluster() throws Exception {
		MvcResult result;

		result = mockMvc.perform(
			get("/api/trib/create/params")
				.param("replicas", "0")
				.param("hostAndPorts", "192.168.33.11:8000-8002")
			)
			.andReturn();
		String body = result.getResponse().getContentAsString();

		result = mockMvc.perform(
			post("/api/trib/create/test")
				.param("params", body)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
			)
			.andReturn();
	}

	private Cluster getCluster(String clusterName) throws Exception {
		MvcResult result = mockMvc.perform(
			get("/api/cluster/test")
			)
			.andReturn();
		return mapper.readValue(result.getResponse().getContentAsString(), Cluster.class);
	}

	@Test
	public void test_clearCluster() throws Exception {
		MvcResult result;

		//		result = mockMvc.perform(
		//			get("/api/trib/create/params")
		//				.param("replicas", "1")
		//				.param("hostAndPorts", "192.168.33.11:8000-8005")
		//			)
		//			.andReturn();
		//		String body = result.getResponse().getContentAsString();
		//
		//		result = mockMvc.perform(
		//			post("/api/trib/create")
		//				.param("params", body)
		//				.header(HttpHeaders.CONTENT_TYPE, "application/json")
		//			)
		//			.andReturn();
	}

	@Test
	public void test_createBasicCluster() throws Exception {
		createBasicCluster();
	}

	@Test
	public void test_createOnlyMasterCluster() throws Exception {
		createOnlyMasterCluster();
	}

	@Test
	public void createParams() throws Exception {
		MvcResult result = mockMvc.perform(
			get("/api/trib/create/params")
				.param("replicas", "1")
				.param("hostAndPorts", "192.168.33.11:8000-8005")
			)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].start_slot_number", is("0")))
			.andExpect(jsonPath("$[0].master", is("192.168.33.11:8000")))
			.andExpect(jsonPath("$[0].replicas[0]", is("192.168.33.11:8003")))
			.andExpect(jsonPath("$[1].start_slot_number", is("5461")))
			.andExpect(jsonPath("$[1].master", is("192.168.33.11:8001")))
			.andExpect(jsonPath("$[1].replicas[0]", is("192.168.33.11:8004")))
			.andExpect(jsonPath("$[2].start_slot_number", is("10922")))
			.andExpect(jsonPath("$[2].master", is("192.168.33.11:8002")))
			.andExpect(jsonPath("$[2].replicas[0]", is("192.168.33.11:8005")))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());
	}

	@Test
	public void createParams_invalid_param() throws Exception {
		MvcResult result;
		result = mockMvc.perform(
			get("/api/trib/create/params")
				.param("replicas", "-1")
				.param("hostAndPorts", "192.168.33.11:8000-8005")
			)
			.andExpect(status().is(400))
			.andExpect(jsonPath("$.error.code", is("400_000")))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		result = mockMvc.perform(
			get("/api/trib/create/params")
				.param("replicas", "100")
				.param("hostAndPorts", "192.168.33.11:8000-8005")
			)
			.andExpect(status().is(400))
			.andExpect(jsonPath("$.error.code", is("400_000")))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		result = mockMvc.perform(
			get("/api/trib/create/params")
				.param("replicas", "1")
				.param("hostAndPorts", "192.168.33.11:8000-8006")
			)
			.andExpect(status().is(400))
			.andExpect(jsonPath("$.error.code", is("400_000")))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		result = mockMvc.perform(
			get("/api/trib/create/params")
				.param("replicas", "1")
				.param("hostAndPorts", "192.168.33.11:8000-8004,192.168.33.11:7000")
			)
			.andExpect(status().is(400))
			.andExpect(jsonPath("$.error.code", is("400_000")))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		result = mockMvc.perform(
			get("/api/trib/create/params")
				.param("replicas", "1")
				.param("hostAndPorts", "192.168.33.11:8000-8004,192.168.33.11:9000")
			)
			.andExpect(status().is(400))
			.andExpect(jsonPath("$.error.code", is("400_000")))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());
	}

	@Test
	public void createWithClusterName() throws Exception {
		MvcResult result;

		result = mockMvc.perform(
			get("/api/trib/create/params")
				.param("replicas", "1")
				.param("hostAndPorts", "192.168.33.11:8000-8005")
			)
			.andReturn();
		String body = result.getResponse().getContentAsString();

		result = mockMvc.perform(
			post("/api/trib/create/test")
				.param("params", body)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
			)
			.andExpect(jsonPath("$.nodes[0].host_and_port", is(testRedisEmptyCluster)))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		result = mockMvc.perform(
			post("/api/trib/create/test")
				.param("replicas", "1")
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
				.content(body)
			)
			.andExpect(status().is(400))
			.andExpect(jsonPath("$.error.code", is("400_000")))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());
	}

	@Test
	public void create() throws Exception {
		MvcResult result;

		result = mockMvc.perform(
			get("/api/trib/create/params")
				.param("replicas", "1")
				.param("hostAndPorts", "192.168.33.11:8000-8005")
			)
			.andReturn();
		String body = result.getResponse().getContentAsString();

		result = mockMvc.perform(
			post("/api/trib/create")
				.param("params", body)
				.header(HttpHeaders.CONTENT_TYPE, "application/json")
			)
			.andExpect(jsonPath("$.nodes[0].host_and_port", is(testRedisEmptyCluster)))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());
	}

	@Test
	public void check() throws Exception {
		createBasicCluster();

		MvcResult result;
		result = mockMvc.perform(
			get("/api/trib/check")
				.param("hostAndPort", testRedisEmptyCluster)
			)
			.andExpect(jsonPath("$.errors").isArray())
			.andReturn();
		log.debug(result.getResponse().getContentAsString());
	}

	@Test
	public void fix() throws Exception {
		createBasicCluster();

		MvcResult result;
		result = mockMvc.perform(
			get("/api/trib/check")
				.param("hostAndPort", testRedisEmptyCluster)
			)
			.andExpect(jsonPath("$.errors").isArray())
			.andReturn();
		log.debug(result.getResponse().getContentAsString());
	}

	@Test
	public void reshard() throws Exception {
		createBasicCluster();

		MvcResult result;
		Cluster cluster = getCluster("test");

		// Success
		ClusterNode toNode = cluster.getNodes().stream().filter(node -> {
			return StringUtils.isBlank(node.getMasterNodeId());
		}).findFirst().get();

		result = mockMvc.perform(
			post("/api/trib/reshard")
				.param("hostAndPort", testRedisEmptyCluster)
				.param("slotCount", "100")
				.param("fromNodeIds", "ALL")
				.param("toNodeId", toNode.getNodeId())
			)
			.andExpect(jsonPath("$.nodes[0].served_slots").value("0-5509,10922-10972"))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		// Invalid
		toNode = cluster.getNodes().stream().filter(node -> {
			return StringUtils.isNotBlank(node.getMasterNodeId());
		}).findFirst().get();

		result = mockMvc.perform(
			post("/api/trib/reshard")
				.param("hostAndPort", testRedisEmptyCluster)
				.param("slotCount", "100")
				.param("fromNodeIds", "ALL")
				.param("toNodeId", toNode.getNodeId())
			)
			.andExpect(status().is(400))
			.andExpect(jsonPath("$.error.code").value("400_000"))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

	}

	@Test
	public void addNodeAndDeleteNode() throws Exception {
		createOnlyMasterCluster();

		MvcResult result;
		Cluster cluster;

		// Add
		cluster = getCluster("test");

		result = mockMvc.perform(
			post("/api/trib/add-node")
				.param("hostAndPort", testRedisEmptyCluster)
				.param("newHostAndPort", "192.168.33.11:8003")
			)
			.andExpect(jsonPath("$.nodes[3].flags[0]").value("master"))
			.andExpect(jsonPath("$.nodes[3].served_slots").value(""))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		result = mockMvc.perform(
			post("/api/trib/add-node")
				.param("hostAndPort", testRedisEmptyCluster)
				.param("newHostAndPort", "192.168.33.11:8004")
				.param("masterNodeId", cluster.getNodes().get(0).getNodeId())
			)
			.andExpect(jsonPath("$.nodes[4].flags[0]").value("slave"))
			.andExpect(jsonPath("$.nodes[4].master_node_id").value(cluster.getNodes().get(0).getNodeId()))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		cluster = getCluster("test");
		result = mockMvc.perform(
			post("/api/trib/add-node")
				.param("hostAndPort", testRedisEmptyCluster)
				.param("newHostAndPort", "192.168.33.11:8005")
				.param("masterNodeId", cluster.getNodes().get(3).getNodeId())
			)
			.andExpect(jsonPath("$.nodes[5].flags[0]").value("slave"))
			.andExpect(jsonPath("$.nodes[5].master_node_id").value(cluster.getNodes().get(3).getNodeId()))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		//Delete 
		cluster = getCluster("test");

		result = mockMvc.perform(
			post("/api/trib/delete-node")
				.param("hostAndPort", testRedisEmptyCluster)
				.param("nodeId", cluster.getNodes().get(3).getNodeId())
			)
			.andExpect(jsonPath("$.nodes[3].host_and_port").value("192.168.33.11:8004"))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		result = mockMvc.perform(
			post("/api/trib/delete-node")
				.param("hostAndPort", testRedisEmptyCluster)
				.param("nodeId", cluster.getNodes().get(4).getNodeId())
			)
			.andExpect(jsonPath("$.nodes[3].host_and_port").value("192.168.33.11:8005"))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		result = mockMvc.perform(
			post("/api/trib/delete-node")
				.param("hostAndPort", testRedisEmptyCluster)
				.param("nodeId", cluster.getNodes().get(5).getNodeId())
			)
			.andExpect(jsonPath("$.nodes[3]").doesNotExist())
			.andReturn();
		log.debug(result.getResponse().getContentAsString());
	}

	@Test
	public void replicateNode() throws Exception {
		createBasicCluster();

		MvcResult result;
		Cluster cluster;

		cluster = getCluster("test");
		result = mockMvc.perform(
			post("/api/trib/replicate")
				.param("hostAndPort", "192.168.33.11:8003")
				.param("masterNodeId", cluster.getNodeByHostAndPort("192.168.33.11:8002").getNodeId())
			)
			.andExpect(jsonPath("$.slots[2].replicas[0].host_and_port").value("192.168.33.11:8003"))
			.andExpect(jsonPath("$.slots[2].replicas[1]").exists())
			.andReturn();
		log.debug(result.getResponse().getContentAsString());
	}

	@Test
	public void replicateEmptyMasterNode() throws Exception {
		createOnlyMasterCluster();

		MvcResult result;
		Cluster cluster;

		result = mockMvc.perform(
			post("/api/trib/add-node")
				.param("hostAndPort", testRedisEmptyCluster)
				.param("newHostAndPort", "192.168.33.11:8003")
			)
			.andExpect(jsonPath("$.nodes[3].flags[0]").value("master"))
			.andExpect(jsonPath("$.nodes[3].served_slots").value(""))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());

		cluster = getCluster("test");
		result = mockMvc.perform(
			post("/api/trib/replicate")
				.param("hostAndPort", "192.168.33.11:8003")
				.param("masterNodeId", cluster.getNodeByHostAndPort("192.168.33.11:8002").getNodeId())
			)
			.andExpect(jsonPath("$.slots[2].replicas[0]").exists())
			.andExpect(jsonPath("$.slots[2].replicas[0].host_and_port").value("192.168.33.11:8003"))
			.andReturn();
		log.debug(result.getResponse().getContentAsString());
	}
}
