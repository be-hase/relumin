package com.behase.relumin.controller;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.LoginUser;
import com.behase.relumin.model.Role;
import com.behase.relumin.service.LoggingOperationService;
import com.behase.relumin.service.UserService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class UserApiControllerTest {
    @InjectMocks
    @Spy
    private UserApiController controller = new UserApiController();

    @Mock
    private UserService userService;

    @Mock
    private LoggingOperationService loggingOperationService;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void users() throws Exception {
        doReturn(Lists.newArrayList()).when(userService).getUsers();

        List<LoginUser> result = controller.users();
        log.info("result={}", result);
        assertThat(result, is(empty()));
    }

    @Test
    public void add() throws Exception {
        doReturn(new LoginUser("username", "displayName", "rawPassword", Role.VIEWER.getAuthority())).when(userService).getUser(anyString());

        LoginUser result = controller.add(null, "", "", "", "");
        log.info("result={}", result);
        assertThat(result.getUsername(), is("username"));
    }

    @Test
    public void update() throws Exception {
        doReturn(new LoginUser("username", "displayName", "rawPassword", Role.VIEWER.getAuthority())).when(userService).getUser(anyString());

        LoginUser result = controller.update(null, "", "", "");
        log.info("result={}", result);
        assertThat(result.getUsername(), is("username"));
    }

    @Test
    public void updateMe_authentication_is_null_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("You are not loggedin."));

        controller.updateMe(null, "");
    }

    @Test
    public void updateMe() throws Exception {
        doReturn(new LoginUser("username", "displayName", "rawPassword", Role.VIEWER.getAuthority())).when(userService).getUser(anyString());

        LoginUser result = controller.updateMe(mock(Authentication.class), "");
        log.info("result={}", result);
        assertThat(result.getUsername(), is("username"));
    }

    @Test
    public void changePassword_authentication_is_null_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("You are not loggedin."));

        controller.changePassword(null, "", "");
    }

    @Test
    public void changePassword() throws Exception {
        doReturn(new LoginUser("username", "displayName", "rawPassword", Role.VIEWER.getAuthority())).when(userService).getUser(anyString());

        LoginUser result = controller.changePassword(mock(Authentication.class), "", "");
        log.info("result={}", result);
        assertThat(result.getUsername(), is("username"));
    }

    @Test
    public void delete() throws Exception {
        Map<String, Boolean> result = controller.delete(null, "");
        log.info("result={}", result);
        assertThat(result.get("isSuccess"), is(true));
    }
}
