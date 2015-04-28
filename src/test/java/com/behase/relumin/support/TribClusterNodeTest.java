package com.behase.relumin.support;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisDataException;

public class TribClusterNodeTest {
	private TribClusterNode tribClusterNode;

	@Before
	public void before() {
		tribClusterNode = new TribClusterNode("192.168.33.11:7000");
	}

	@After
	public void after() throws Exception {
		tribClusterNode.close();
	}

	@Test
	public void connect() {
		tribClusterNode.connect();
	}

	@Test(expected = IllegalArgumentException.class)
	public void connect_invalid_host_and_port() {
		tribClusterNode = new TribClusterNode("192.168.33.11:70000");
		tribClusterNode.connect();
	}

	@Test
	public void assertCluster() {
		tribClusterNode.connect();
		tribClusterNode.assertCluster();
	}

	@Test(expected = IllegalArgumentException.class)
	public void assertCluster_not_cluster_mode() {
		tribClusterNode = new TribClusterNode("192.168.33.10:6379");
		tribClusterNode.connect();
		tribClusterNode.assertCluster();
	}

	@Test
	public void assertEmpty() {
		tribClusterNode = new TribClusterNode("192.168.33.11:7006");
		tribClusterNode.connect();
		tribClusterNode.assertEmpty();
	}

	@Test(expected = IllegalArgumentException.class)
	public void assertEmpty_already_knows_other_cluster() {
		tribClusterNode.connect();
		tribClusterNode.assertEmpty();
	}

	@Test(expected = JedisDataException.class)
	public void assertEmpty_not_cluster() {
		tribClusterNode = new TribClusterNode("192.168.33.10:6379");
		tribClusterNode.connect();
		tribClusterNode.assertEmpty();
	}
}
