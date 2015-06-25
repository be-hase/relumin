package com.behase.relumin;

import org.junit.Test;

import com.behase.relumin.model.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TmpTest {

	@Test
	public void test() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(new LoginUser("hoge", "aa", "ADMIN"));
		log.debug("{}", json);

		LoginUser user = mapper.readValue(json, LoginUser.class);
		log.debug("{}", mapper.writeValueAsString(user));
	}
}
