package com.behase.relumin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class LoginUser {
	private String username;
	private String displayName;
	private String password;
	private String role;

	public LoginUser(String username, String displayName, String rawPassword, String role) {
		this.username = username;
		this.displayName = displayName;
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
