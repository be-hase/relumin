package com.behase.relumin.scheduler;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeSchedulerTest {
	private NodeScheduler nodeScheduler;

	@Before
	public void setUp() {
		nodeScheduler = new NodeScheduler();
	}

	@Test
	public void test() throws InterruptedException {
		int i = 0;
		long sum = 0;
		while (true) {
			log.debug("i={}, sum={}", i, sum);
			sum += System.currentTimeMillis();
			i++;
		}
	}

	@Test
	public void isNotify() {
		assertThat(nodeScheduler.isNotify("string", "eq", "hoge", "hoge"), is(true));
		assertThat(nodeScheduler.isNotify("string", "ne", "hogo", "hoge"), is(true));
		assertThat(nodeScheduler.isNotify("string", "lt", "hoge", "hoge"), is(false));

		assertThat(nodeScheduler.isNotify("number", "eq", "10", "10"), is(true));
		assertThat(nodeScheduler.isNotify("number", "eq", "1", "1.00000"), is(true));
		assertThat(nodeScheduler.isNotify("number", "eq", "0.000000000001", "0.000000000001"), is(true));

		assertThat(nodeScheduler.isNotify("number", "ne", "1", "1.1"), is(true));
		assertThat(nodeScheduler.isNotify("number", "ne", "0.000000000001", "0.00000000001"), is(true));

		assertThat(nodeScheduler.isNotify("number", "gt", "1.0000000000000000001", "1"), is(true));
		assertThat(nodeScheduler.isNotify("number", "gt", "1", "1"), is(false));
		assertThat(nodeScheduler.isNotify("number", "gt", "0", "1"), is(false));

		assertThat(nodeScheduler.isNotify("number", "ge", "1.0000000000000000001", "1"), is(true));
		assertThat(nodeScheduler.isNotify("number", "ge", "1", "1"), is(true));
		assertThat(nodeScheduler.isNotify("number", "ge", "0", "1"), is(false));

		assertThat(nodeScheduler.isNotify("number", "lt", "1.0000000000000000001", "1"), is(false));
		assertThat(nodeScheduler.isNotify("number", "lt", "1", "1"), is(false));
		assertThat(nodeScheduler.isNotify("number", "lt", "0", "1"), is(true));

		assertThat(nodeScheduler.isNotify("number", "le", "1.0000000000000000001", "1"), is(false));
		assertThat(nodeScheduler.isNotify("number", "le", "1", "1"), is(true));
		assertThat(nodeScheduler.isNotify("number", "le", "0", "1"), is(true));
	}
}
