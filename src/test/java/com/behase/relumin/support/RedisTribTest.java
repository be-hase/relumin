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

	@Value("${test.redis.emptyStandAlone}")
	private String testRedisEmptyStandAlone;

	@Value("${test.redis.emptyStandAloneAll}")
	private String testRedisEmptyStandAloneAll;

	@Value("${test.redis.normalStandAlone}")
	private String testRedisNormalStandAlone;

	@Value("${test.redis.normalStandAloneAll}")
	private String testRedisNormalStandAloneAll;

	private RedisTrib redisTrib;

	@Before
	public void before() {
		// empty
		for (String item : StringUtils.split(testRedisEmptyClusterAll, ",")) {
			try (Jedis jedis = JedisUtils.getJedisByHostAndPort(item)) {
				jedis.flushAll();
			}
		}
		for (String item : StringUtils.split(testRedisNormalStandAloneAll, ",")) {
			try (Jedis jedis = JedisUtils.getJedisByHostAndPort(item)) {
				jedis.flushAll();
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
	}

	@Test
	public void getCreateClusterParam() throws Exception {
		redisTrib = new RedisTrib();
		List<CreateClusterParam> result = redisTrib.getCreateClusterParam(1, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisEmptyClusterAll, ","))));
		log.debug("getRecommendCreateClusterParam result = {}", result);
	}

	@Test(expected = InvalidParameterException.class)
	public void getCreateClusterParam_redis_is_not_cluster() throws Exception {
		redisTrib = new RedisTrib();
		redisTrib.getCreateClusterParam(0, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisEmptyStandAloneAll, ","))));
	}

	@Test(expected = InvalidParameterException.class)
	public void getCreateClusterParam_redis_is_not_empty() throws Exception {
		redisTrib = new RedisTrib();
		redisTrib.getCreateClusterParam(0, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisNormalCluster, ","))));
	}

	@Test(expected = InvalidParameterException.class)
	public void getCreateClusterParam_not_enough_master() throws Exception {
		redisTrib = new RedisTrib();
		redisTrib.getCreateClusterParam(2, Sets.newTreeSet(Arrays.asList(StringUtils.split(testRedisEmptyClusterAll, ","))));
	}
}
