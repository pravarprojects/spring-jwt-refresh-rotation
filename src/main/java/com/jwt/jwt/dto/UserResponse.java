package com.jwt.jwt.dto;

import com.jwt.jwt.entity.role;

public class UserResponse {
	public int id;
	public String name;
	public String username;
	public role role;
	public String password;
	
	public UserResponse(int id, String name, String username, com.jwt.jwt.entity.role role, String password) {
		super();
		this.id = id;
		this.name = name;
		this.username = username;
		this.role = role;
		this.password = password;
	}
	public UserResponse (String name, String username, com.jwt.jwt.entity.role role, String password) {
		super();
		this.name = name;
		this.username = username;
		this.role = role;
		this.password = password;
	}
	public UserResponse() {
		super();
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
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
		return "UserResponse [id=" + id + ", name=" + name + ", username=" + username + ", role=" + role + ", password="
				+ password + "]";
	}
	
	
}
