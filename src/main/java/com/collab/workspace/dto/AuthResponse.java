package com.collab.workspace.dto;

public class AuthResponse {

	private String token;
	private String tokenType;
	private String name;
	private String email;
	private String headline;
	private String bio;
	private String location;
	private String accentColor;
	private boolean profilePublic;
	private boolean emailNotifications;
	private boolean workspaceDigest;
	private boolean focusModeEnabled;

	public AuthResponse() {
	}

	public AuthResponse(
		String token,
		String tokenType,
		String name,
		String email,
		String headline,
		String bio,
		String location,
		String accentColor,
		boolean profilePublic,
		boolean emailNotifications,
		boolean workspaceDigest,
		boolean focusModeEnabled
	) {
		this.token = token;
		this.tokenType = tokenType;
		this.name = name;
		this.email = email;
		this.headline = headline;
		this.bio = bio;
		this.location = location;
		this.accentColor = accentColor;
		this.profilePublic = profilePublic;
		this.emailNotifications = emailNotifications;
		this.workspaceDigest = workspaceDigest;
		this.focusModeEnabled = focusModeEnabled;
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

	public String getHeadline() {
		return headline;
	}

	public void setHeadline(String headline) {
		this.headline = headline;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getAccentColor() {
		return accentColor;
	}

	public void setAccentColor(String accentColor) {
		this.accentColor = accentColor;
	}

	public boolean isProfilePublic() {
		return profilePublic;
	}

	public void setProfilePublic(boolean profilePublic) {
		this.profilePublic = profilePublic;
	}

	public boolean isEmailNotifications() {
		return emailNotifications;
	}

	public void setEmailNotifications(boolean emailNotifications) {
		this.emailNotifications = emailNotifications;
	}

	public boolean isWorkspaceDigest() {
		return workspaceDigest;
	}

	public void setWorkspaceDigest(boolean workspaceDigest) {
		this.workspaceDigest = workspaceDigest;
	}

	public boolean isFocusModeEnabled() {
		return focusModeEnabled;
	}

	public void setFocusModeEnabled(boolean focusModeEnabled) {
		this.focusModeEnabled = focusModeEnabled;
	}
}
