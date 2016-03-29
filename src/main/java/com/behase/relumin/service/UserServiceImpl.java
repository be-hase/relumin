package com.behase.relumin.service;

import com.behase.relumin.Constants;
import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.LoginUser;
import com.behase.relumin.model.Role;
import com.behase.relumin.util.ValidationUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    public static final TypeReference<List<LoginUser>> LIST_LOGIN_USER_TYPE = new TypeReference<List<LoginUser>>() {
    };

    @Autowired
    private JedisPool dataStoreJedisPool;

    @Autowired
    private ObjectMapper mapper;

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
        return users.stream().filter(v -> StringUtils.equalsIgnoreCase(username, v.getUsername())).findFirst().orElse(null);
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
    public void addUser(String username, String displayName, String password, String role) throws Exception {
        ValidationUtils.username(username);

        ValidationUtils.notBlank(displayName, "displayName");
        if (displayName.length() > 255) {
            throw new InvalidParameterException("displayName must be less than 256.");
        }

        ValidationUtils.notBlank(password, "password");
        if (password.length() < 8) {
            throw new InvalidParameterException("username must be more than or equal to 8.");
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
        users.add(new LoginUser(username, displayName, password, role));
        try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
            String key = Constants.getUsersRedisKey(redisPrefixKey);
            String json = mapper.writeValueAsString(users);
            dataStoreJedis.set(key, json);
        }
    }

    @Override
    public void changePassword(String username, String oldPassword, String password) throws Exception {
        ValidationUtils.notBlank(password, "password");
        if (password.length() < 8) {
            throw new InvalidParameterException("username must be more than 8.");
        }
        if (password.length() > 255) {
            throw new InvalidParameterException("username must be less than 256.");
        }

        List<LoginUser> users = getUsers();
        LoginUser user = getUser(users, username);

        StandardPasswordEncoder encoder = new StandardPasswordEncoder();
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidParameterException(String.format("Old password does not match."));
        }

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
        LoginUser user = getUser(users, username);

        users.remove(user);

        try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
            String key = Constants.getUsersRedisKey(redisPrefixKey);
            String json = mapper.writeValueAsString(users);
            dataStoreJedis.set(key, json);
        }
    }

    @Override
    public void updateUser(String username, String displayName, String role) throws Exception {
        List<LoginUser> users = getUsers();
        LoginUser user = getUser(users, username);

        if (displayName.length() > 255) {
            throw new InvalidParameterException("displayName must be less than 256.");
        }

        if (StringUtils.isNotBlank(displayName)) {
            user.setDisplayName(displayName);
        }
        if (StringUtils.isNotBlank(role)) {
            user.setRole(Role.get(role).getAuthority());
        }

        try (Jedis dataStoreJedis = dataStoreJedisPool.getResource()) {
            String key = Constants.getUsersRedisKey(redisPrefixKey);
            String json = mapper.writeValueAsString(users);
            dataStoreJedis.set(key, json);
        }
    }

    private LoginUser getUser(List<LoginUser> users, String username) {
        return users.stream().filter(v -> {
            return StringUtils.equalsIgnoreCase(username, v.getUsername());
        }).findFirst().orElseThrow(() -> {
            return new InvalidParameterException(String.format("'%s' does not exist.", username));
        });
    }
}
