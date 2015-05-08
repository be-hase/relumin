package com.behase.relumin.support;

import java.util.Arrays;

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
import com.behase.relumin.util.JedisUtils;

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
	public void getAllocSlotsForCreateCluster() throws RedisTribException {
		redisTrib = new RedisTrib();
		redisTrib.getAllocSlotsForCreateCluster(1, Arrays.asList(StringUtils.split(testRedisEmptyClusterAll, ",")));
	}

	@Test
	public void allocSlots1() throws RedisTribException {
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
			redisTrib.addNodes(node);
		}

		redisTrib.allocSlots();
	}

	@Test
	public void allocSlots2() throws RedisTribException {
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
			redisTrib.addNodes(node);
		}

		redisTrib.allocSlots();
	}

	@Test
	public void allocSlots3() throws RedisTribException {
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
			redisTrib.addNodes(node);
		}

		redisTrib.allocSlots();
	}

	@Test
	public void allocSlots4() throws RedisTribException {
		redisTrib = new RedisTrib();
		Whitebox.setInternalState(redisTrib, "replicas", 6);
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
			redisTrib.addNodes(node);
		}

		redisTrib.allocSlots();
	}

	@Test(expected = RedisTribException.class)
	public void getAllocSlotsForCreateCluster_redis_is_not_cluster() throws RedisTribException {
		redisTrib = new RedisTrib();
		redisTrib.getAllocSlotsForCreateCluster(0, Arrays.asList(StringUtils.split(testRedisEmptyStandAloneAll, ",")));
	}

	@Test(expected = RedisTribException.class)
	public void getAllocSlotsForCreateCluster_redis_is_not_empty() throws RedisTribException {
		redisTrib = new RedisTrib();
		redisTrib.getAllocSlotsForCreateCluster(0, Arrays.asList(StringUtils.split(testRedisNormalCluster, ",")));
	}

	@Test(expected = RedisTribException.class)
	public void getAllocSlotsForCreateCluster_not_enough_master() throws RedisTribException {
		redisTrib = new RedisTrib();
		redisTrib.getAllocSlotsForCreateCluster(2, Arrays.asList(StringUtils.split(testRedisEmptyClusterAll, ",")));
	}
}
