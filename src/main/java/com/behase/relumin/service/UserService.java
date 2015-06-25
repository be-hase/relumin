package com.behase.relumin.service;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.behase.relumin.model.LoginUser;

public interface UserService extends UserDetailsService {
	LoginUser getUser(String username) throws Exception;

	List<LoginUser> getUsers() throws Exception;

	void addUser(String username, String password, String role) throws Exception;

	void changePassword(String username, String password) throws Exception;

	void deleteUser(String username) throws Exception;
}
