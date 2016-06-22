package com.behase.relumin.service;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.model.SlowLog;
import com.behase.relumin.support.JedisSupport;
import com.behase.relumin.webconfig.WebConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.test.OutputCapture;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Slowlog;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class NodeServiceImplTest {
    @InjectMocks
    @Spy
    private NodeServiceImpl service = new NodeServiceImpl();

    @Mock
    private JedisPool dataStoreJedisPool;

    @Spy
    private ObjectMapper mapper = WebConfig.MAPPER;

    @Mock
    private JedisSupport jedisSupport;

    @Mock
    private Jedis dataStoreJedis;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Before
    public void init() {
        Whitebox.setInternalState(service, "redisPrefixKey", "_relumin");
        doReturn(dataStoreJedis).when(dataStoreJedisPool).getResource();
    }

    @Test
    public void getStaticsInfo() throws Exception {
        Jedis jedis = mock(Jedis.class);

        doReturn(jedis).when(jedisSupport).getJedisByHostAndPort(anyString());
        doReturn(ImmutableMap.of("hoge", "hoge")).when(jedisSupport).parseInfoResult(anyString());

        Map<String, String> result = service.getStaticsInfo(new ClusterNode());
        log.info("result={}", result);
        assertThat(result, is(ImmutableMap.of("hoge", "hoge")));
    }

    @Test
    public void getStaticsInfoHistory_end_is_smaller_than_start() {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("End time must be larger than start time"));

        service.getStaticsInfoHistory("clusterName", "nodeId1", Lists.newArrayList("used_memory"), 10, 0);
    }

    @Test
    public void getStaticsInfoHistory() {
        List<Map<String, String>> staticsList1 = Lists.newArrayList(
                ImmutableMap.of(
                        "hoge", "10",
                        "_timestamp", "10"
                ),
                ImmutableMap.of(
                        "hoge", "9",
                        "_timestamp", "9"
                ),
                ImmutableMap.of(
                        "hoge", "8",
                        "_timestamp", "8"
                )
        );
        List<Map<String, String>> staticsList2 = Lists.newArrayList(
                ImmutableMap.of(
                        "hoge", "7",
                        "_timestamp", "7"
                ),
                ImmutableMap.of(
                        "hoge", "6",
                        "_timestamp", "6"
                ),
                ImmutableMap.of(
                        "hoge", "5",
                        "_timestamp", "5"
                )
        );
        List<Map<String, String>> staticsList3 = Lists.newArrayList(
                ImmutableMap.of(
                        "hoge", "4",
                        "_timestamp", "4"
                ),
                ImmutableMap.of(
                        "hoge", "3",
                        "_timestamp", "3"
                ),
                ImmutableMap.of(
                        "hoge", "2",
                        "_timestamp", "2"
                )
        );

        doReturn(staticsList1).when(service).getStaticsInfoHistoryFromRedis(anyString(), anyString(), anyList(), eq(0L), eq(999L));
        doReturn(staticsList2).when(service).getStaticsInfoHistoryFromRedis(anyString(), anyString(), anyList(), eq(1000L), eq(1999L));
        doReturn(staticsList3).when(service).getStaticsInfoHistoryFromRedis(anyString(), anyString(), anyList(), eq(2000L), eq(2999L));

        Map<String, List<List<Object>>> result = service.getStaticsInfoHistory("clusterName", "nodeId1", Lists.newArrayList("hoge"), 4, 8);
        log.info("result={}", result);
        assertThat(result, is(
                ImmutableMap.of("hoge", Lists.newArrayList(
                        Lists.newArrayList(4L, 4.0),
                        Lists.newArrayList(5L, 5.0),
                        Lists.newArrayList(6L, 6.0),
                        Lists.newArrayList(7L, 7.0),
                        Lists.newArrayList(8L, 8.0)
                ))
        ));
    }

    @Test
    public void getSlowLog() {
        // given
        Jedis jedis = mock(Jedis.class);
        doReturn(jedis).when(jedisSupport).getJedisByHostAndPort(anyString());
        List<Slowlog> slowlogs = Lists.newArrayList(mock(Slowlog.class), mock(Slowlog.class));
        doReturn(slowlogs).when(jedis).slowlogGet();

        // when
        List<SlowLog> result = service.getSlowLogAndReset(new ClusterNode());

        // then
        assertThat(result.size(), is(2));
    }

    @Test
    public void getThresholdMillis() {
        // smaller than or equal to 1 day
        assertThat(service.getThresholdMillis(0, (long) 1 * 24 * 60 * 60 * 1000), is(Long.MIN_VALUE));

        // smaller than or equal to 7 days
        assertThat(service.getThresholdMillis(0, (long) 7 * 24 * 60 * 60 * 1000), is((long) 5 * 60 * 1000));

        // smaller than or equal to 30 days
        assertThat(service.getThresholdMillis(0, (long) 30 * 24 * 60 * 60 * 1000), is((long) 30 * 60 * 1000));

        // smaller than or equal to 60 days
        assertThat(service.getThresholdMillis(0, (long) 60 * 24 * 60 * 60 * 1000), is((long) 1 * 60 * 60 * 1000));

        // smaller than or equal to 120 days
        assertThat(service.getThresholdMillis(0, (long) 120 * 24 * 60 * 60 * 1000), is((long) 2 * 60 * 60 * 1000));

        // smaller than or equal to 180 days
        assertThat(service.getThresholdMillis(0, (long) 180 * 24 * 60 * 60 * 1000), is((long) 6 * 60 * 60 * 1000));

        // smaller than or equal to 1 year
        assertThat(service.getThresholdMillis(0, (long) 365 * 24 * 60 * 60 * 1000), is((long) 12 * 60 * 60 * 1000));

        // else
        assertThat(service.getThresholdMillis(0, (long) 366 * 24 * 60 * 60 * 1000), is((long) 24 * 60 * 60 * 1000));
    }

    @Test
    public void getAveragedStaticsInfoHistory() throws Exception {
        List<Map<String, String>> rangeResult = Lists.newArrayList(
                ImmutableMap.of(
                        "hoge", "1",
                        "_timestamp", "100"
                ),
                ImmutableMap.of(
                        "hoge", "2",
                        "_timestamp", "200"
                ),
                ImmutableMap.of(
                        "hoge", "3",
                        "_timestamp", "300"
                ),
                ImmutableMap.of(
                        "hoge", "4",
                        "_timestamp", "400"
                ),
                ImmutableMap.of(
                        "hoge", "5",
                        "_timestamp", "500"
                ),
                ImmutableMap.of(
                        "hoge", "6",
                        "_timestamp", "600"
                )
        );

        Map<String, List<List<Object>>> result;

        result = service.getAveragedStaticsInfoHistory(rangeResult, Lists.newArrayList("hoge"), Long.MIN_VALUE);
        log.info("result={}", result);
        assertThat(result, is(
                ImmutableMap.of("hoge", Lists.newArrayList(
                        Lists.newArrayList(100L, 1.0),
                        Lists.newArrayList(200L, 2.0),
                        Lists.newArrayList(300L, 3.0),
                        Lists.newArrayList(400L, 4.0),
                        Lists.newArrayList(500L, 5.0),
                        Lists.newArrayList(600L, 6.0)
                ))
        ));

        result = service.getAveragedStaticsInfoHistory(rangeResult, Lists.newArrayList("hoge"), 100);
        log.info("result={}", result);
        assertThat(result, is(
                ImmutableMap.of("hoge", Lists.newArrayList(
                        Lists.newArrayList(100L, 1.0),
                        Lists.newArrayList(200L, 2.0),
                        Lists.newArrayList(300L, 3.0),
                        Lists.newArrayList(400L, 4.0),
                        Lists.newArrayList(500L, 5.0),
                        Lists.newArrayList(600L, 6.0)
                ))
        ));

        result = service.getAveragedStaticsInfoHistory(rangeResult, Lists.newArrayList("hoge"), 101);
        log.info("result={}", result);
        assertThat(result, is(
                ImmutableMap.of("hoge", Lists.newArrayList(
                        Lists.newArrayList(150L, 1.5),
                        Lists.newArrayList(350L, 3.5),
                        Lists.newArrayList(550L, 5.5)
                ))
        ));

        result = service.getAveragedStaticsInfoHistory(rangeResult, Lists.newArrayList("hoge"), 201);
        log.info("result={}", result);
        assertThat(result, is(
                ImmutableMap.of("hoge", Lists.newArrayList(
                        Lists.newArrayList(200L, 2.0),
                        Lists.newArrayList(500L, 5.0)
                ))
        ));

        result = service.getAveragedStaticsInfoHistory(rangeResult, Lists.newArrayList("hoge"), Long.MAX_VALUE);
        log.info("result={}", result);
        // Above expression cannot use. Variable Type issue.
        Map<String, List<List<Object>>> expected = Maps.newHashMap();
        List<List<Object>> hogeValues = Lists.newArrayList();
        List<Object> hogeValue1 = Lists.newArrayList(350L, 3.5);
        hogeValues.add(hogeValue1);
        expected.put("hoge", hogeValues);
        assertThat(result, is(expected));
    }

    @Test
    public void shutdown_redis_cannot_be_connected() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("Failed to connect to Redis Cluster"));

        Jedis jedis = mock(Jedis.class);

        doReturn(jedis).when(jedisSupport).getJedisByHostAndPort(anyString());
        doThrow(Exception.class).when(jedis).ping();

        service.shutdown("localhost:10000");
    }

    @Test
    public void shutdown() throws Exception {
        Jedis jedis = mock(Jedis.class);

        doReturn(jedis).when(jedisSupport).getJedisByHostAndPort(anyString());

        service.shutdown("localhost:10000");
    }

    @Test
    public void getStaticsInfoHistoryFromRedis() throws Exception {
        List<String> rawResult = Lists.newArrayList(
                "{\"hoge\":\"1\",\"_timestamp\":\"0\"}",
                "{\"hoge\":\"2\",\"_timestamp\":\"0\"}"
        );

        doReturn(rawResult).when(dataStoreJedis).lrange(anyString(), anyLong(), anyLong());

        List<Map<String, String>> result = service.getStaticsInfoHistoryFromRedis("clusterName", "nodeId1", Lists.newArrayList("hoge"), 0, 2);
        log.info("result={}", result);
        assertThat(result, is(
                Lists.newArrayList(
                        ImmutableMap.of("hoge", "1", "_timestamp", "0"),
                        ImmutableMap.of("hoge", "2", "_timestamp", "0")
                )
        ));
    }
}
