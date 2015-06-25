package com.behase.relumin.service;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.behase.relumin.Constants;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.LoginUser;
import com.behase.relumin.util.ValidationUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

@Service
public class UserServiceImpl implements UserService {
	public static final TypeReference<List<LoginUser>> LIST_LOGIN_USER_TYPE = new TypeReference<List<LoginUser>>() {
	};

	@Autowired
	JedisPool dataStoreJedisPool;

	@Autowired
	ObjectMapper mapper;

	@Value("${redis.prefixKey}")
	private String redisPrefixKey;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		try {
			LoginUser user = getUser(username);
			if (user == null) {
				throw new UsernameNotFoundException("Not found.");
			}
			return user.getSpringUser();
		} catch (Exception e) {
			throw new UsernameNotFoundException(e.getMessage(), e);
		}
	}

	@Override
	public LoginUser getUser(String username) throws Exception {
		List<LoginUser> users = getUsers();
		if (users == null) {
			return null;
		}
		return users.stream().filter(v -> {
			return StringUtils.equalsIgnoreCase(username, v.getUsername());
		}).findFirst().orElse(null);
	}

	@Override
	public List<LoginUser> getUsers() throws Exception {
		String key = Constants.getUsersRedisKey(redisPrefixKey);
		try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			String json = dataStoreJedis.get(key);
			if (json == null) {
				return Lists.newArrayList();
			}
			return mapper.readValue(json, LIST_LOGIN_USER_TYPE);
		}
	}

	@Override
	public void addUser(String username, String password, String role) throws Exception {
		ValidationUtils.notBlank(username, "username");
		if (username.length() > 255) {
			throw new InvalidParameterException("username must be less than 256.");
		}

		ValidationUtils.notBlank(password, "password");
		if (password.length() < 8) {
			throw new InvalidParameterException("username must be more than 8.");
		}
		if (password.length() > 255) {
			throw new InvalidParameterException("username must be less than 256.");
		}

		ValidationUtils.notBlank(role, "role");

		LoginUser exist = getUser(username);
		if (exist != null) {
			throw new InvalidParameterException(String.format("'%s' already exists.", username));
		}

		List<LoginUser> users = getUsers();
		if (users == null) {
			users = Lists.newArrayList();
		}
		users.add(new LoginUser(username, password, role));
		try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			String key = Constants.getUsersRedisKey(redisPrefixKey);
			String json = mapper.writeValueAsString(users);
			dataStoreJedis.set(key, json);
		}
	}

	@Override
	public void changePassword(String username, String password) throws Exception {
		List<LoginUser> users = getUsers();
		LoginUser user = users.stream().filter(v -> {
			return StringUtils.equalsIgnoreCase(username, v.getUsername());
		}).findFirst().orElseThrow(() -> {
			throw new InvalidParameterException(String.format("'%s does not exist.'", username));
		});

		user.setRawPassword(password);

		try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			String key = Constants.getUsersRedisKey(redisPrefixKey);
			String json = mapper.writeValueAsString(users);
			dataStoreJedis.set(key, json);
		}
	}

	@Override
	public void deleteUser(String username) throws Exception {
		List<LoginUser> users = getUsers();
		LoginUser user = users.stream().filter(v -> {
			return StringUtils.equalsIgnoreCase(username, v.getUsername());
		}).findFirst().orElseThrow(() -> {
			throw new InvalidParameterException(String.format("'%s does not exist.'", username));
		});

		users.remove(user);

		try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
			String key = Constants.getUsersRedisKey(redisPrefixKey);
			String json = mapper.writeValueAsString(users);
			dataStoreJedis.set(key, json);
		}
	}
}
