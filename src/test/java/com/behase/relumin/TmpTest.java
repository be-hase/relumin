package com.behase.relumin;

import org.junit.Test;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TmpTest {

	@Test
	public void test() throws Exception {
		log.debug("{}", new StandardPasswordEncoder().encode("hoge"));
		log.debug("{}", new StandardPasswordEncoder().encode("hoge"));
		log.debug("{}", new StandardPasswordEncoder().encode("hoge"));
		log.debug("{}", new StandardPasswordEncoder().encode("hoge"));
	}
}
