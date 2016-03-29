package com.behase.relumin.service;

import com.behase.relumin.support.JedisSupport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class RedisTribServiceImplTest {
    private RedisTribServiceImpl service;
    private JedisSupport jedisSupport;

    @Before
    public void init() {
        service = spy(new RedisTribServiceImpl());
        jedisSupport = mock(JedisSupport.class);

        inject();
    }

    private void inject() {
        Whitebox.setInternalState(service, "jedisSupport", jedisSupport);
    }

    @Test
    public void test() {
        // this class is just a wrapper of redis-trib.
    }
}
