package com.behase.relumin.model;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginUser {
	private String username;
	private String password;
	private String role;

	public LoginUser(String username, String rawPassword, String role) {
		this.username = username;
		this.password = new StandardPasswordEncoder().encode(rawPassword);
		this.role = Role.get(role).getAuthority();
	}

	@JsonIgnore
	public User getSpringUser() {
		return new User(username, password, Lists.newArrayList(Role.get(role)));
	}

	public void setRawPassword(String rawPassword) {
		this.password = new StandardPasswordEncoder().encode(rawPassword);
	}
}
