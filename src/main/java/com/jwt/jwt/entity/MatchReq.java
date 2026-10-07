package com.jwt.jwt.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchReq {
	private String Username;
    private String Token;
    
	public MatchReq(String Username, String Token) {
		super();
		this.Username = Username;
		this.Token = Token;
	}
	public String getUsername() {
		return Username;
	}
	public void setUsername(String username) {
		Username = username;
	}
	public String getToken() {
		return Token;
	}
	public void setToken(String token) {
		Token = token;
	}
    
}
