package com.behase.relumin.service;

import com.behase.relumin.support.JedisSupport;
import com.behase.relumin.webconfig.WebConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.boot.test.OutputCapture;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static org.mockito.Mockito.*;

public class NodeServiceImplTest {
    private NodeServiceImpl service;
    private JedisPool dataStoreJedisPool;
    private ObjectMapper mapper;
    private JedisSupport jedisSupport;
    private String redisPrefixKey;
    private Jedis dataStoreJedis;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public OutputCapture capture = new OutputCapture();

    @Before
    public void init() {
        service = spy(new NodeServiceImpl());
        dataStoreJedisPool = mock(JedisPool.class);
        mapper = WebConfig.MAPPER;
        jedisSupport = mock(JedisSupport.class);
        redisPrefixKey = "_relumin";
        dataStoreJedis = mock(Jedis.class);

        inject();
    }

    private void inject() {
        Whitebox.setInternalState(service, "dataStoreJedisPool", dataStoreJedisPool);
        Whitebox.setInternalState(service, "mapper", mapper);
        Whitebox.setInternalState(service, "jedisSupport", jedisSupport);
        Whitebox.setInternalState(service, "jedisSupport", jedisSupport);
        Whitebox.setInternalState(service, "redisPrefixKey", redisPrefixKey);

        doReturn(dataStoreJedis).when(dataStoreJedisPool).getResource();
    }
}
