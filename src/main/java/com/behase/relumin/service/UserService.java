package com.behase.relumin.service;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.behase.relumin.model.LoginUser;

public interface UserService extends UserDetailsService {
	LoginUser getUser(String username) throws Exception;

	List<LoginUser> getUsers() throws Exception;

	void addUser(String username, String displayName, String password, String role) throws Exception;

	void changePassword(String username, String oldPassword, String password) throws Exception;

	void updateUser(String username, String displayName, String role) throws Exception;

	void deleteUser(String username) throws Exception;
}
