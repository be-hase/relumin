package com.behase.relumin.config;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

@Slf4j
public class ReluminConfigTest {
	@Test
	public void test() throws Exception {
		ReluminConfig config = ReluminConfig.create("relumin-local-conf.yml");
		log.debug("config = {}", config);
	}
}
