package com.behase.relumin.service;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.LoginUser;
import com.behase.relumin.model.Role;
import com.behase.relumin.webconfig.WebConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {
    @InjectMocks
    @Spy
    private UserServiceImpl service = new UserServiceImpl();

    @Mock
    private JedisPool dataStoreJedisPool;

    @Spy
    private ObjectMapper mapper = WebConfig.MAPPER;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public OutputCapture capture = new OutputCapture();

    private Jedis dataStoreJedis = mock(Jedis.class);

    @Before
    public void init() {
        Whitebox.setInternalState(service, "redisPrefixKey", "_relumin");
        doReturn(dataStoreJedis).when(dataStoreJedisPool).getResource();
    }

    @Test
    public void loadUserByUsername_user_is_null_then_throw_exception() throws Exception {
        expectedEx.expect(UsernameNotFoundException.class);
        expectedEx.expectMessage(containsString("Not found"));

        doReturn(null).when(service).getUser(anyString());

        service.loadUserByUsername("username");
    }

    @Test
    public void loadUserByUsername() throws Exception {
        LoginUser loginUser = new LoginUser("username", "displayName", "rawPassword", Role.VIEWER.getAuthority());

        doReturn(loginUser).when(service).getUser(anyString());

        UserDetails result = service.loadUserByUsername("username");
        log.info("result={}", result);
        assertThat(result.getUsername(), is("username"));
    }

    @Test
    public void getUser_users_is_null_then_return_null() throws Exception {
        doReturn(null).when(service).getUsers();

        LoginUser result = service.getUser("username");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void getUser() throws Exception {
        List<LoginUser> users = Lists.newArrayList(
                new LoginUser("username1", "displayName1", "rawPassword", Role.VIEWER.getAuthority()),
                new LoginUser("username2", "displayName2", "rawPassword", Role.VIEWER.getAuthority()),
                new LoginUser("username3", "displayName3", "rawPassword", Role.VIEWER.getAuthority())
        );

        doReturn(users).when(service).getUsers();

        LoginUser result = service.getUser("username2");
        log.info("result={}", result);
        assertThat(result.getUsername(), is("username2"));
    }

    @Test
    public void getUsers_json_is_null_then_return_empty_list() throws Exception {
        doReturn(null).when(dataStoreJedis).get(anyString());
        assertThat(service.getUsers(), is(empty()));
    }

    @Test
    public void getUsers() throws Exception {
        List<LoginUser> users = Lists.newArrayList(
                new LoginUser("username1", "displayName1", "rawPassword", Role.VIEWER.getAuthority()),
                new LoginUser("username2", "displayName2", "rawPassword", Role.VIEWER.getAuthority()),
                new LoginUser("username3", "displayName3", "rawPassword", Role.VIEWER.getAuthority())
        );

        doReturn(mapper.writeValueAsString(users)).when(dataStoreJedis).get(anyString());

        assertThat(service.getUsers(), is(users));
    }

    @Test
    public void addUser_invalid_username_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("User ID is invalid.");

        service.addUser("username!", "displayName", "password", Role.VIEWER.getAuthority());
    }

    @Test
    public void addUser_displayName_is_blank_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("displayName must not be blank");

        service.addUser("username", "", "password", Role.VIEWER.getAuthority());
    }

    @Test
    public void addUser_displayName_is_greater_than_255_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("displayName must be less than or equal to 255");

        service.addUser("username", RandomStringUtils.randomAlphanumeric(256), "password", Role.VIEWER.getAuthority());
    }

    @Test
    public void addUser_password_is_blank_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("password must not be blank");

        service.addUser("username", "displayName", "", Role.VIEWER.getAuthority());
    }

    @Test
    public void addUser_password_is_smaller_than_8_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("password must be more than or equal to 8");

        service.addUser("username", "displayName", "1234567", Role.VIEWER.getAuthority());
    }

    @Test
    public void addUser_password_is_greater_than_255_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("password must be less than or equal to 255");

        service.addUser("username", "displayName", RandomStringUtils.randomAlphanumeric(256), Role.VIEWER.getAuthority());
    }

    @Test
    public void addUser_role_is_blank_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("role must not be blank");

        service.addUser("username", "displayName", "password", "");
    }

    @Test
    public void addUser_user_already_exists_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("already exists");

        doReturn(new LoginUser()).when(service).getUser(anyString());

        service.addUser("username", "displayName", "password", Role.VIEWER.getAuthority());
    }

    @Test
    public void addUser() throws Exception {
        doReturn(null).when(service).getUser(anyString());
        doReturn(null).when(service).getUsers();

        service.addUser("username", "displayName", "password", Role.VIEWER.getAuthority());
    }

    @Test
    public void changePassword_password_is_blank_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("password must not be blank");

        service.changePassword("username", "oldPassword", "");
    }

    @Test
    public void changePassword_password_is_smaller_than_8_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("password must be more than or equal to 8");

        service.changePassword("username", "oldPassword", "1234567");
    }

    @Test
    public void changePassword_password_is_greater_than_255_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("password must be less than or equal to 255");

        service.changePassword("username", "oldPassword", RandomStringUtils.randomAlphanumeric(256));
    }

    @Test
    public void changePassword_oldPassword_is_wrong_then_throw_expection() throws Exception {
        doReturn(Lists.newArrayList()).when(service).getUsers();
        doReturn(
                new LoginUser("username", "displayName", "oldPassword", Role.VIEWER.getAuthority()))
                .when(service).getUser(anyList(), anyString());

        service.changePassword("username", "oldPassword", "password");
    }

    @Test
    public void deleteUser() throws Exception {
        doReturn(Lists.newArrayList()).when(service).getUsers();
        doReturn(new LoginUser()).when(service).getUser(anyList(), anyString());

        service.deleteUser("username");
    }

    @Test
    public void updateUser_displayName_is_greater_than_255_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("displayName must be less than or equal to 255");

        service.updateUser("username", RandomStringUtils.randomAlphanumeric(256), Role.VIEWER.getAuthority());
    }

    @Test
    public void updateUser() throws Exception {
        doReturn(Lists.newArrayList()).when(service).getUsers();
        doReturn(new LoginUser()).when(service).getUser(anyList(), anyString());

        service.updateUser("username", "displayName", Role.VIEWER.getAuthority());
    }

    @Test
    public void getUser_with_users_username() throws Exception {
        List<LoginUser> users = Lists.newArrayList(
                new LoginUser("username1", "displayName1", "rawPassword", Role.VIEWER.getAuthority()),
                new LoginUser("username2", "displayName2", "rawPassword", Role.VIEWER.getAuthority()),
                new LoginUser("username3", "displayName3", "rawPassword", Role.VIEWER.getAuthority())
        );

        assertThat(service.getUser(users, "username2").getUsername(), is("username2"));
    }

    @Test
    public void getUser_with_users_username_does_not_exist_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage("does not exist");

        List<LoginUser> users = Lists.newArrayList(
                new LoginUser("username1", "displayName1", "rawPassword", Role.VIEWER.getAuthority()),
                new LoginUser("username2", "displayName2", "rawPassword", Role.VIEWER.getAuthority()),
                new LoginUser("username3", "displayName3", "rawPassword", Role.VIEWER.getAuthority())
        );
        service.getUser(users, "hoge");
    }
}


