package com.behase.relumin.config;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReluminConfigTest {
	@Test
	public void test() throws JsonParseException, JsonMappingException, IOException {
		ReluminConfig config = ReluminConfig.create("relumin-local-conf.yml");
		log.debug("config = {}", config);
	}
}
