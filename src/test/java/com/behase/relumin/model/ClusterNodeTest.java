package com.behase.relumin.model;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.mockito.Spy;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Slf4j
public class ClusterNodeTest {
    @Spy
    private ClusterNode clusterNode = new ClusterNode();

    @Test
    public void hasFlag() {
        clusterNode.setFlags(null);
        assertThat(clusterNode.hasFlag("hoge"), is(false));

        clusterNode.setFlags(Sets.newHashSet());
        assertThat(clusterNode.hasFlag("hoge"), is(false));

        clusterNode.setFlags(Sets.newHashSet("hoge"));
        assertThat(clusterNode.hasFlag("hoge"), is(true));
    }

    @Test
    public void getServedSlots() {
        String result;

        clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1)));
        result = clusterNode.getServedSlots();
        log.info("result={}", result);
        assertThat(result, is("1"));

        clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 2, 3, 4)));
        result = clusterNode.getServedSlots();
        log.info("result={}", result);
        assertThat(result, is("1-4"));

        clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 2, 3, 4, 11, 12, 13)));
        result = clusterNode.getServedSlots();
        log.info("result={}", result);
        assertThat(result, is("1-4,11-13"));

        clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 2, 3, 4, 13)));
        result = clusterNode.getServedSlots();
        log.info("result={}", result);
        assertThat(result, is("1-4,13"));

        clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 2, 5, 7, 8, 9, 10)));
        result = clusterNode.getServedSlots();
        log.info("result={}", result);
        assertThat(result, is("1-2,5,7-10"));

        clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 2, 5, 7, 8, 9, 10, 12)));
        result = clusterNode.getServedSlots();
        log.info("result={}", result);
        assertThat(result, is("1-2,5,7-10,12"));

        clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 3)));
        result = clusterNode.getServedSlots();
        log.info("result={}", result);
        assertThat(result, is("1,3"));

        clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 3, 5)));
        result = clusterNode.getServedSlots();
        log.info("result={}", result);
        assertThat(result, is("1,3,5"));

        clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 3, 5, 4, 2)));
        result = clusterNode.getServedSlots();
        log.info("result={}", result);
        assertThat(result, is("1-5"));

        clusterNode.setServedSlotsSet(Sets.newTreeSet(Sets.newHashSet(1, 3, 5, 6, 7, 8)));
        result = clusterNode.getServedSlots();
        log.info("result={}", result);
        assertThat(result, is("1,3,5-8"));
    }

    @Test
    public void getSlotCount() {
        clusterNode.setServedSlotsSet(Sets.newHashSet(1, 2));
        assertThat(clusterNode.getSlotCount(), is(2));
    }

    @Test
    public void getHost() {
        clusterNode.setHostAndPort("localhost:8080");
        assertThat(clusterNode.getHost(), is("localhost"));
    }

    @Test
    public void getPort() {
        clusterNode.setHostAndPort("localhost:8080");
        assertThat(clusterNode.getPort(), is(8080));
    }

    @Test
    public void equals() {
        clusterNode.setNodeId("1");

        assertThat(clusterNode.equals(clusterNode), is(true));
        assertThat(clusterNode.equals(null), is(false));
        assertThat(clusterNode.equals("hoge"), is(false));

        ClusterNode clusterNode2 = new ClusterNode();
        clusterNode2.setNodeId("2");
        assertThat(clusterNode.equals(clusterNode2), is(false));

        clusterNode2.setNodeId("1");
        assertThat(clusterNode.equals(clusterNode2), is(true));
    }
}
