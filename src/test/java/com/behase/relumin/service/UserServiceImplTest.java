package com.behase.relumin.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.JedisPool;

@Slf4j
public class UserServiceImplTest {
    private JedisPool dataStoreJedisPool;
    private String redisPrefixKey;
    private int hoge = 0;

    @Before
    public void init() {

    }

    @Test
    public void test1() {
        log.info("hoge={}", hoge++);
        log.info("hoge={}", hoge++);
    }

    @Test
    public void test2() {
        log.info("hoge={}", hoge++);
    }
}
