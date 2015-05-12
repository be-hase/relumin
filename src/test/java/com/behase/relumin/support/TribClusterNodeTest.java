package com.behase.relumin.support;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster.Reset;
import redis.clients.jedis.exceptions.JedisDataException;

import com.behase.relumin.Application;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.util.JedisUtils;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@ActiveProfiles("test")
public class TribClusterNodeTest {
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

	private TribClusterNode tribClusterNode;

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

		tribClusterNode = new TribClusterNode(testRedisNormalCluster);
	}

	@After
	public void after() throws Exception {
		tribClusterNode.close();
	}

	@Test
	public void connect() throws Exception {
		tribClusterNode.connect(true);
	}

	@Test(expected = InvalidParameterException.class)
	public void connect_invalid_host_and_port() throws Exception {
		tribClusterNode = new TribClusterNode("192.168.33.11:80");
		tribClusterNode.connect(true);
	}

	@Test
	public void connect_invalid_host_and_port_then_through() throws Exception {
		tribClusterNode = new TribClusterNode("192.168.33.11:80");
		tribClusterNode.connect(false);
	}

	@Test
	public void assertCluster() throws Exception {
		tribClusterNode.connect();
		tribClusterNode.assertCluster();
	}

	@Test(expected = InvalidParameterException.class)
	public void assertCluster_not_cluster_mode() throws Exception {
		tribClusterNode = new TribClusterNode(testRedisEmptyStandAlone);
		tribClusterNode.connect();
		tribClusterNode.assertCluster();
	}

	@Test
	public void assertEmpty() throws Exception {
		tribClusterNode = new TribClusterNode(testRedisEmptyCluster);
		tribClusterNode.connect();
		tribClusterNode.assertEmpty();
	}

	@Test(expected = InvalidParameterException.class)
	public void assertEmpty_already_knows_other_cluster() throws Exception {
		tribClusterNode.connect();
		tribClusterNode.assertEmpty();
	}

	@Test(expected = JedisDataException.class)
	public void assertEmpty_not_cluster() throws Exception {
		tribClusterNode = new TribClusterNode(testRedisEmptyStandAlone);
		tribClusterNode.connect();
		tribClusterNode.assertEmpty();
	}

	@Test
	public void loadInfo() throws Exception {
		tribClusterNode.loadInfo();
		log.debug("config signature={}", tribClusterNode.getConfigSignature());
	}

	@Test
	public void addSlots() {
		assertThat(tribClusterNode.isDirty(), is(false));
		tribClusterNode.addTmpSlots(Lists.newArrayList(1, 2, 3));
		assertThat(tribClusterNode.isDirty(), is(true));
	}
}
