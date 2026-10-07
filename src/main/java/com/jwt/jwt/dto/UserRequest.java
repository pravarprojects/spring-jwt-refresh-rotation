package com.jwt.jwt.dto;

import com.jwt.jwt.entity.role;

public class UserRequest {
	public String name;
	public String username;
	public role role;
	public String password;
	public UserRequest(String name, String username, com.jwt.jwt.entity.role role, String password) {
		super();
		this.name = name;
		this.username = username;
		this.role = role;
		this.password = password;
	}
	public UserRequest() {
		super();
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public role getRole() {
		return role;
	}
	public void setRole(role role) {
		this.role = role;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	@Override
	public String toString() {
		return "UserRequest [name=" + name + ", username=" + username + ", role=" + role + ", password=" + password
				+ "]";
	}
	
}
