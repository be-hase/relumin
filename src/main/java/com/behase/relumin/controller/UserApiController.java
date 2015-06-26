package com.behase.relumin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.LoginUser;
import com.behase.relumin.service.UserService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

//@Slf4j
@RestController
@RequestMapping(value = "/api")
public class UserApiController {
	@Autowired
	UserService userService;

	@RequestMapping(value = "/users", method = RequestMethod.GET)
	public Object users(
			Authentication authentication
			) throws Exception {
		List<LoginUser> users = userService.getUsers();
		return users == null ? Lists.newArrayList() : users;
	}

	@RequestMapping(value = "/user/{username}", method = RequestMethod.POST)
	public Object add(
			Authentication authentication,
			@PathVariable String username,
			@RequestParam(defaultValue = "") String displayName,
			@RequestParam(defaultValue = "") String role,
			@RequestParam(defaultValue = "") String password
			) throws Exception {
		userService.addUser(username, displayName, password, role);
		return userService.getUser(username);
	}

	@RequestMapping(value = "/user/{username}/update", method = RequestMethod.POST)
	public Object update(
			@PathVariable String username,
			@RequestParam(defaultValue = "") String displayName,
			@RequestParam(defaultValue = "") String role
			) throws Exception {
		userService.updateUser(username, displayName, role);
		return userService.getUser(username);
	}

	@RequestMapping(value = "/me/update", method = RequestMethod.POST)
	public Object updateMe(
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
	public Object changePassword(
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
	public Object delete(
			@PathVariable String username
			) throws Exception {
		userService.deleteUser(username);

		Map<String, Boolean> result = Maps.newHashMap();
		result.put("isSuccess", true);
		return result;
	}
}
