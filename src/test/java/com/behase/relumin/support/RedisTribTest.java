package com.behase.relumin.support;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster.Reset;

import com.behase.relumin.Application;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.util.JedisUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@ActiveProfiles("test")
public class RedisTribTest {
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

	private RedisTrib redisTrib;

	@Before
	public void before() {
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
		for (String item : StringUtils.split(testRedisEmptyStandAloneAll, ",")) {
			try (Jedis jedis = JedisUtils.getJedisByHostAndPort(item)) {
				jedis.flushAll();
			} catch (Exception e) {
			}
		}
	}

	@After
	public void after() throws Exception {
		if (redisTrib != null) {
			redisTrib.close();
		}
	}

	@Test
	public void test() {
	}

	@Test
	public void buildCreateClusterParam1() throws Exception {
		redisTrib = new RedisTrib();
		Whitebox.setInternalState(redisTrib, "replicas", 1);
		String[] hostAndPorts = {
			"2.2.2.2:7", "2.2.2.2:8", "2.2.2.2:9", "2.2.2.2:10", "2.2.2.2:11", "2.2.2.2:12",
			"2.2.2.2:1", "2.2.2.2:2", "2.2.2.2:3", "2.2.2.2:4", "2.2.2.2:5", "2.2.2.2:6",
			"1.1.1.1:7", "1.1.1.1:8", "1.1.1.1:9", "1.1.1.1:10", "1.1.1.1:11", "1.1.1.1:12",
			"1.1.1.1:1", "1.1.1.1:2", "1.1.1.1:3", "1.1.1.1:4", "1.1.1.1:5", "1.1.1.1:6",
		};
		for (String hostAndPort : hostAndPorts) {
			TribClusterNode node = new TribClusterNode(hostAndPort);
			node.getInfo().setNodeId("nodeId:" + hostAndPort);
			redisTrib.addNodes(node);
		}

		redisTrib.allocSlots();

		List<CreateClusterParam> params = redisTrib.buildCreateClusterParam();
		CreateClusterParam param;

		assertThat(params.size(), is(12));

		param = params.get(0);
		assertThat(param.getStartSlotNumber(), is("0"));
		assertThat(param.getEndSlotNumber(), is("1364"));
		assertThat(param.getMaster(), is("1.1.1.1:1"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("2.2.2.2:7")));
		param = params.get(1);
		assertThat(param.getStartSlotNumber(), is("1365"));
		assertThat(param.getEndSlotNumber(), is("2729"));
		assertThat(param.getMaster(), is("2.2.2.2:1"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:7")));
		param = params.get(10);
		assertThat(param.getStartSlotNumber(), is("13650"));
		assertThat(param.getEndSlotNumber(), is("15014"));
		assertThat(param.getMaster(), is("1.1.1.1:6"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("2.2.2.2:12")));
		param = params.get(11);
		assertThat(param.getStartSlotNumber(), is("15015"));
		assertThat(param.getEndSlotNumber(), is("16383"));
		assertThat(param.getMaster(), is("2.2.2.2:6"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:12")));
	}

	@Test
	public void buildCreateClusterParam2() throws Exception {
		redisTrib = new RedisTrib();
		Whitebox.setInternalState(redisTrib, "replicas", 2);
		String[] hostAndPorts = {
			"1.1.1.1:1", "1.1.1.1:2", "1.1.1.1:3", "1.1.1.1:4", "1.1.1.1:5", "1.1.1.1:6",
			"1.1.1.1:7", "1.1.1.1:8", "1.1.1.1:9", "1.1.1.1:10", "1.1.1.1:11", "1.1.1.1:12",
			"2.2.2.2:1", "2.2.2.2:2", "2.2.2.2:3", "2.2.2.2:4", "2.2.2.2:5", "2.2.2.2:6",
			"2.2.2.2:7", "2.2.2.2:8", "2.2.2.2:9", "2.2.2.2:10", "2.2.2.2:11", "2.2.2.2:12",
			"3.3.3.3:1", "3.3.3.3:2", "3.3.3.3:3", "3.3.3.3:4", "3.3.3.3:5", "3.3.3.3:6",
			"3.3.3.3:7", "3.3.3.3:8", "3.3.3.3:9", "3.3.3.3:10", "3.3.3.3:11", "3.3.3.3:12",
		};
		for (String hostAndPort : hostAndPorts) {
			TribClusterNode node = new TribClusterNode(hostAndPort);
			node.getInfo().setNodeId("nodeId:" + hostAndPort);
			redisTrib.addNodes(node);
		}

		redisTrib.allocSlots();

		List<CreateClusterParam> params = redisTrib.buildCreateClusterParam();
		CreateClusterParam param;

		assertThat(params.size(), is(12));

		param = params.get(0);
		assertThat(param.getStartSlotNumber(), is("0"));
		assertThat(param.getEndSlotNumber(), is("1364"));
		assertThat(param.getMaster(), is("1.1.1.1:1"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("2.2.2.2:5", "3.3.3.3:5")));
		param = params.get(1);
		assertThat(param.getStartSlotNumber(), is("1365"));
		assertThat(param.getEndSlotNumber(), is("2729"));
		assertThat(param.getMaster(), is("2.2.2.2:1"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:5", "3.3.3.3:6")));
		param = params.get(10);
		assertThat(param.getStartSlotNumber(), is("13650"));
		assertThat(param.getEndSlotNumber(), is("15014"));
		assertThat(param.getMaster(), is("2.2.2.2:4"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:11", "3.3.3.3:12")));
		param = params.get(11);
		assertThat(param.getStartSlotNumber(), is("15015"));
		assertThat(param.getEndSlotNumber(), is("16383"));
		assertThat(param.getMaster(), is("3.3.3.3:4"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:12", "2.2.2.2:12")));
	}

	@Test
	public void buildCreateClusterParam3() throws Exception {
		redisTrib = new RedisTrib();
		Whitebox.setInternalState(redisTrib, "replicas", 1);
		String[] hostAndPorts = {
			"1.1.1.1:1", "1.1.1.1:2", "1.1.1.1:3", "1.1.1.1:4", "1.1.1.1:5", "1.1.1.1:6",
			"2.2.2.2:1", "2.2.2.2:2", "2.2.2.2:3", "2.2.2.2:4", "2.2.2.2:5", "2.2.2.2:6",
			"2.2.2.2:7", "2.2.2.2:8", "2.2.2.2:9", "2.2.2.2:10", "2.2.2.2:11", "2.2.2.2:12",
			"3.3.3.3:1", "3.3.3.3:2", "3.3.3.3:3", "3.3.3.3:4", "3.3.3.3:5", "3.3.3.3:6",
			"3.3.3.3:7", "3.3.3.3:8", "3.3.3.3:9", "3.3.3.3:10", "3.3.3.3:11", "3.3.3.3:12",
		};
		for (String hostAndPort : hostAndPorts) {
			TribClusterNode node = new TribClusterNode(hostAndPort);
			node.getInfo().setNodeId("nodeId:" + hostAndPort);
			redisTrib.addNodes(node);
		}

		redisTrib.allocSlots();

		List<CreateClusterParam> params = redisTrib.buildCreateClusterParam();
		CreateClusterParam param;

		assertThat(params.size(), is(15));
		param = params.get(0);
		assertThat(param.getStartSlotNumber(), is("0"));
		assertThat(param.getEndSlotNumber(), is("1091"));
		assertThat(param.getMaster(), is("1.1.1.1:1"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("2.2.2.2:6")));
		param = params.get(1);
		assertThat(param.getStartSlotNumber(), is("1092"));
		assertThat(param.getEndSlotNumber(), is("2183"));
		assertThat(param.getMaster(), is("2.2.2.2:1"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:6")));
		param = params.get(13);
		assertThat(param.getStartSlotNumber(), is("14196"));
		assertThat(param.getEndSlotNumber(), is("15287"));
		assertThat(param.getMaster(), is("2.2.2.2:5"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("3.3.3.3:12")));
		param = params.get(14);
		assertThat(param.getStartSlotNumber(), is("15288"));
		assertThat(param.getEndSlotNumber(), is("16383"));
		assertThat(param.getMaster(), is("3.3.3.3:5"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("2.2.2.2:12")));
	}

	@Test
	public void buildCreateClusterParam4() throws Exception {
		redisTrib = new RedisTrib();
		Whitebox.setInternalState(redisTrib, "replicas", 7);
		String[] hostAndPorts = {
			"1.1.1.1:1", "1.1.1.1:2", "1.1.1.1:3", "1.1.1.1:4", "1.1.1.1:5", "1.1.1.1:6",
			"1.1.1.1:7", "1.1.1.1:8", "1.1.1.1:9", "1.1.1.1:10", "1.1.1.1:11", "1.1.1.1:12",
			"2.2.2.2:1", "2.2.2.2:2", "2.2.2.2:3", "2.2.2.2:4", "2.2.2.2:5", "2.2.2.2:6",
			"2.2.2.2:7", "2.2.2.2:8", "2.2.2.2:9", "2.2.2.2:10", "2.2.2.2:11", "2.2.2.2:12",
			"3.3.3.3:1", "3.3.3.3:2", "3.3.3.3:3", "3.3.3.3:4", "3.3.3.3:5", "3.3.3.3:6",
			"3.3.3.3:7", "3.3.3.3:8", "3.3.3.3:9", "3.3.3.3:10", "3.3.3.3:11", "3.3.3.3:12",
		};
		for (String hostAndPort : hostAndPorts) {
			TribClusterNode node = new TribClusterNode(hostAndPort);
			node.getInfo().setNodeId("nodeId:" + hostAndPort);
			redisTrib.addNodes(node);
		}

		redisTrib.allocSlots();

		List<CreateClusterParam> params = redisTrib.buildCreateClusterParam();
		CreateClusterParam param;

		assertThat(params.size(), is(4));
		param = params.get(0);
		assertThat(param.getStartSlotNumber(), is("0"));
		assertThat(param.getEndSlotNumber(), is("4095"));
		assertThat(param.getMaster(), is("1.1.1.1:1"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:3", "1.1.1.1:4", "2.2.2.2:2", "2.2.2.2:3", "2.2.2.2:4", "3.3.3.3:2", "3.3.3.3:3", "3.3.3.3:11")));
		param = params.get(1);
		assertThat(param.getStartSlotNumber(), is("4096"));
		assertThat(param.getEndSlotNumber(), is("8191"));
		assertThat(param.getMaster(), is("2.2.2.2:1"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:5", "1.1.1.1:6", "1.1.1.1:12", "2.2.2.2:5", "2.2.2.2:6", "3.3.3.3:4", "3.3.3.3:5", "3.3.3.3:6")));
		param = params.get(2);
		assertThat(param.getStartSlotNumber(), is("8192"));
		assertThat(param.getEndSlotNumber(), is("12287"));
		assertThat(param.getMaster(), is("3.3.3.3:1"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:7", "1.1.1.1:8", "1.1.1.1:9", "2.2.2.2:7", "2.2.2.2:8", "2.2.2.2:12", "3.3.3.3:7", "3.3.3.3:8")));
		param = params.get(3);
		assertThat(param.getStartSlotNumber(), is("12288"));
		assertThat(param.getEndSlotNumber(), is("16383"));
		assertThat(param.getMaster(), is("1.1.1.1:2"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("1.1.1.1:10", "1.1.1.1:11", "2.2.2.2:9", "2.2.2.2:10", "2.2.2.2:11", "3.3.3.3:9", "3.3.3.3:10", "3.3.3.3:12")));
	}

	@Test
	public void getCreateClusterParam() throws Exception {
		redisTrib = new RedisTrib();
		List<CreateClusterParam> result = redisTrib.getCreateClusterParams(1, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisEmptyClusterAll, ","))));
		log.debug("getRecommendCreateClusterParam result = {}", result);
		List<CreateClusterParam> params = redisTrib.buildCreateClusterParam();

		CreateClusterParam param;
		param = params.get(0);
		assertThat(param.getStartSlotNumber(), is("0"));
		assertThat(param.getEndSlotNumber(), is("5460"));
		assertThat(param.getMaster(), is("192.168.33.11:8000"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("192.168.33.11:8003")));
		param = params.get(1);
		assertThat(param.getStartSlotNumber(), is("5461"));
		assertThat(param.getEndSlotNumber(), is("10921"));
		assertThat(param.getMaster(), is("192.168.33.11:8001"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("192.168.33.11:8004")));
		param = params.get(2);
		assertThat(param.getStartSlotNumber(), is("10922"));
		assertThat(param.getEndSlotNumber(), is("16383"));
		assertThat(param.getMaster(), is("192.168.33.11:8002"));
		assertThat(param.getReplicas(), is(Lists.newArrayList("192.168.33.11:8005")));
	}

	@Test(expected = InvalidParameterException.class)
	public void getCreateClusterParam_redis_is_not_cluster() throws Exception {
		redisTrib = new RedisTrib();
		redisTrib.getCreateClusterParams(0, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisEmptyStandAloneAll, ","))));
	}

	@Test(expected = InvalidParameterException.class)
	public void getCreateClusterParam_redis_is_not_empty() throws Exception {
		redisTrib = new RedisTrib();
		redisTrib.getCreateClusterParams(0, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisNormalCluster, ","))));
	}

	@Test(expected = InvalidParameterException.class)
	public void getCreateClusterParam_not_enough_master() throws Exception {
		redisTrib = new RedisTrib();
		redisTrib.getCreateClusterParams(2, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisEmptyClusterAll, ","))));
	}

	@Test
	public void createCluster() throws Exception {
		redisTrib = new RedisTrib();

		CreateClusterParam param1 = new CreateClusterParam();
		param1.setStartSlotNumber("0");
		param1.setEndSlotNumber("5460");
		param1.setMaster("192.168.33.11:8000");
		param1.setReplicas(Lists.newArrayList("192.168.33.11:8003"));
		CreateClusterParam param2 = new CreateClusterParam();
		param2.setStartSlotNumber("5461");
		param2.setEndSlotNumber("10921");
		param2.setMaster("192.168.33.11:8001");
		param2.setReplicas(Lists.newArrayList("192.168.33.11:8004"));
		CreateClusterParam param3 = new CreateClusterParam();
		param3.setStartSlotNumber("10922");
		param3.setEndSlotNumber("16383");
		param3.setMaster("192.168.33.11:8002");
		param3.setReplicas(Lists.newArrayList("192.168.33.11:8005"));
		List<CreateClusterParam> params = Lists.newArrayList(param1, param2, param3);

		redisTrib.createCluster(params);
	}

	@Test
	public void loadClusterInfoFromNode() {
		redisTrib = new RedisTrib();
		redisTrib.loadClusterInfoFromNode(testRedisNormalCluster);

		List<TribClusterNode> nodes = redisTrib.getNodes();
		nodes.sort((o1, o2) -> {
			return o1.getInfo().getHostAndPort().compareTo(o2.getInfo().getHostAndPort());
		});

		TribClusterNode node;
		node = nodes.get(0);
		assertThat(node.getInfo().getHostAndPort(), is("192.168.33.11:7000"));
		assertThat(node.getInfo().getServedSlots(), is("0-5460"));
		node = nodes.get(1);
		assertThat(node.getInfo().getHostAndPort(), is("192.168.33.11:7001"));
		assertThat(node.getInfo().getServedSlots(), is("5461-10922"));
		node = nodes.get(4);
		assertThat(node.getInfo().getHostAndPort(), is("192.168.33.11:7004"));
		assertThat(node.getInfo().getServedSlots(), is(""));
		assertThat(node.getInfo().getMasterNodeId(), is(nodes.get(1).getInfo().getNodeId()));
		node = nodes.get(5);
		assertThat(node.getInfo().getHostAndPort(), is("192.168.33.11:7005"));
		assertThat(node.getInfo().getServedSlots(), is(""));
		assertThat(node.getInfo().getMasterNodeId(), is(nodes.get(2).getInfo().getNodeId()));
	}
}
