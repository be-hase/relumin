package com.behase.relumin.controller;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.LoginUser;
import com.behase.relumin.service.LoggingOperationService;
import com.behase.relumin.service.UserService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

//@Slf4j
@RestController
@RequestMapping(value = "/api")
public class UserApiController {
    @Autowired
    private UserService userService;

    @Autowired
    private LoggingOperationService loggingOperationService;

    @RequestMapping(value = "/users", method = RequestMethod.GET)
    public List<LoginUser> users() throws Exception {
        List<LoginUser> users = userService.getUsers();
        return users == null ? Lists.newArrayList() : users;
    }

    @RequestMapping(value = "/user/{username}", method = RequestMethod.POST)
    public LoginUser add(
            Authentication authentication,
            @PathVariable String username,
            @RequestParam(defaultValue = "") String displayName,
            @RequestParam(defaultValue = "") String role,
            @RequestParam(defaultValue = "") String password
    ) throws Exception {
        loggingOperationService.log("addUser", authentication, "username={}, displayName={}, role={}.", username, displayName, role);

        userService.addUser(username, displayName, password, role);
        return userService.getUser(username);
    }

    @RequestMapping(value = "/user/{username}/update", method = RequestMethod.POST)
    public LoginUser update(
            Authentication authentication,
            @PathVariable String username,
            @RequestParam(defaultValue = "") String displayName,
            @RequestParam(defaultValue = "") String role
    ) throws Exception {
        loggingOperationService.log("updateUser", authentication, "username={}, displayName={}, role={}.", username, displayName, role);

        userService.updateUser(username, displayName, role);
        return userService.getUser(username);
    }

    @RequestMapping(value = "/me/update", method = RequestMethod.POST)
    public LoginUser updateMe(
            Authentication authentication,
            @RequestParam(defaultValue = "") String displayName
    ) throws Exception {
        if (authentication == null) {
            throw new InvalidParameterException("You are not loggedin.");
        }
        String username = authentication.getName();
        userService.updateUser(username, displayName, null);
        return userService.getUser(username);
    }

    @RequestMapping(value = "/me/change-password", method = RequestMethod.POST)
    public LoginUser changePassword(
            Authentication authentication,
            @RequestParam(defaultValue = "") String oldPassword,
            @RequestParam(defaultValue = "") String password
    ) throws Exception {
        if (authentication == null) {
            throw new InvalidParameterException("You are not loggedin.");
        }
        String username = authentication.getName();
        userService.changePassword(username, oldPassword, password);
        return userService.getUser(username);
    }

    @RequestMapping(value = "/user/{username}/delete", method = RequestMethod.POST)
    public Map<String, Boolean> delete(
            Authentication authentication,
            @PathVariable String username
    ) throws Exception {
        loggingOperationService.log("deleteUser", authentication, "username={}.", username);

        userService.deleteUser(username);

        Map<String, Boolean> result = Maps.newHashMap();
        result.put("isSuccess", true);
        return result;
    }
}
