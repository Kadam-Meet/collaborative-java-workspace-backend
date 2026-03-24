package com.collab.workspace.dto;

public class AuthResponse {

	private String token;
	private String tokenType;
	private String name;
	private String email;

	public AuthResponse() {
	}

	public AuthResponse(String token, String tokenType, String name, String email) {
		this.token = token;
		this.tokenType = tokenType;
		this.name = name;
		this.email = email;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getTokenType() {
		return tokenType;
	}

	public void setTokenType(String tokenType) {
		this.tokenType = tokenType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
