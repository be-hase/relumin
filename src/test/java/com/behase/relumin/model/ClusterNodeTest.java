package com.behase.relumin.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClusterNodeTest {
	private ClusterNode clusterNode;

	@Before
	public void before() {
		clusterNode = new ClusterNode();
	}

	@Test
	public void getServedSlotsSet() {
		String result;

		clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1)));
		result = clusterNode.getServedSlots();
		log.debug("result={}", result);
		assertThat(result, is("1"));

		clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 2, 3, 4)));
		result = clusterNode.getServedSlots();
		log.debug("result={}", result);
		assertThat(result, is("1-4"));

		clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 2, 3, 4, 11, 12, 13)));
		result = clusterNode.getServedSlots();
		log.debug("result={}", result);
		assertThat(result, is("1-4,11-13"));

		clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 2, 3, 4, 13)));
		result = clusterNode.getServedSlots();
		log.debug("result={}", result);
		assertThat(result, is("1-4,13"));

		clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 2, 5, 7, 8, 9, 10)));
		result = clusterNode.getServedSlots();
		log.debug("result={}", result);
		assertThat(result, is("1-2,5,7-10"));

		clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 2, 5, 7, 8, 9, 10, 12)));
		result = clusterNode.getServedSlots();
		log.debug("result={}", result);
		assertThat(result, is("1-2,5,7-10,12"));

		clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 3)));
		result = clusterNode.getServedSlots();
		log.debug("result={}", result);
		assertThat(result, is("1,3"));

		clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 3, 5)));
		result = clusterNode.getServedSlots();
		log.debug("result={}", result);
		assertThat(result, is("1,3,5"));

		clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 3, 5, 4, 2)));
		result = clusterNode.getServedSlots();
		log.debug("result={}", result);
		assertThat(result, is("1-5"));

		clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 3, 5, 6, 7, 8)));
		result = clusterNode.getServedSlots();
		log.debug("result={}", result);
		assertThat(result, is("1,3,5-8"));
	}
}
