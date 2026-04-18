package com.collab.workspace.dto;

public class ApiMessageResponse {

	private String message;

	public ApiMessageResponse() {
	}

	public ApiMessageResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}