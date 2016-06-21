package com.behase.relumin.model;

import org.junit.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LoginUserTest {
    @Test
    public void constructor() {
        LoginUser loginUser = new LoginUser("username", "displayName", "rawPassword",
                Role.VIEWER.getAuthority());
        assertThat(loginUser.getUsername(), is("username"));
        assertThat(loginUser.getDisplayName(), is("displayName"));
        assertThat(new StandardPasswordEncoder().matches("rawPassword", loginUser.getPassword()), is(true));
        assertThat(loginUser.getRole(), is(Role.VIEWER.getAuthority()));
    }

    @Test
    public void setRawPassword() {
        LoginUser loginUser = new LoginUser();
        loginUser.setRawPassword("rawPassword");
        assertThat(new StandardPasswordEncoder().matches("rawPassword", loginUser.getPassword()), is(true));
    }

    @Test
    public void getSpringUser() {
        LoginUser loginUser = new LoginUser("username", "displayName", "rawPassword",
                Role.VIEWER.getAuthority());
        User user = loginUser.getSpringUser();
        assertThat(user.getUsername(), is("username"));
        assertThat(new StandardPasswordEncoder().matches("rawPassword", user.getPassword()), is(true));
        assertThat(user.getAuthorities(), contains(Role.VIEWER));
    }
}
