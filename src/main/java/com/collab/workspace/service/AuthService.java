package com.collab.workspace.service;

import com.collab.workspace.config.JwtUtil;
import com.collab.workspace.dto.AuthResponse;
import com.collab.workspace.dto.LoginRequest;
import com.collab.workspace.dto.SignupRequest;
import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.entity.User;
import com.collab.workspace.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.jwtUtil = jwtUtil;
		this.passwordEncoder = passwordEncoder;
	}

	public AuthResponse signup(SignupRequest request) {
		String email = normalizeEmail(request.getEmail());
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new IllegalArgumentException("User already exists with this email");
		}

		User user = new User();
		user.setName(request.getName().trim());
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setHeadline("Collaborative Java builder");
		user.setBio("Tell your teammates what you are building and how you like to work.");
		user.setAccentColor("emerald");
		user.setCreatedAt(LocalDateTime.now());
		userRepository.save(user);

		String token = jwtUtil.generateToken(email);
		return toAuthResponse(user, token);
	}

	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.getEmail());
		User user = userRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

		boolean validPassword = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
		if (!validPassword && isLegacySha256Match(request.getPassword(), user.getPasswordHash())) {
			user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
			userRepository.save(user);
			validPassword = true;
		}

		if (!validPassword) {
			throw new IllegalArgumentException("Invalid email or password");
		}

		String token = jwtUtil.generateToken(email);
		return toAuthResponse(user, token);
	}

	public AuthResponse me(String email) {
		User user = userRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new IllegalArgumentException("User not found"));
		return toAuthResponse(user, null);
	}

	public AuthResponse updateMe(String email, WorkspaceRequest request) {
		User user = userRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new IllegalArgumentException("User not found"));

		if (request != null && request.getName() != null && !request.getName().isBlank()) {
			user.setName(request.getName().trim());
		}

		if (request != null && request.getHeadline() != null) {
			user.setHeadline(trimToNull(request.getHeadline(), 120));
		}

		if (request != null && request.getBio() != null) {
			user.setBio(trimToNull(request.getBio(), 500));
		}

		if (request != null && request.getLocation() != null) {
			user.setLocation(trimToNull(request.getLocation(), 120));
		}

		if (request != null && request.getAccentColor() != null && !request.getAccentColor().isBlank()) {
			user.setAccentColor(request.getAccentColor().trim().toLowerCase());
		}

		if (request != null && request.getPassword() != null && !request.getPassword().isBlank()) {
			user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		}

		if (request != null && request.getProfilePublic() != null) {
			user.setProfilePublic(request.getProfilePublic());
		}

		if (request != null && request.getEmailNotifications() != null) {
			user.setEmailNotifications(request.getEmailNotifications());
		}

		if (request != null && request.getWorkspaceDigest() != null) {
			user.setWorkspaceDigest(request.getWorkspaceDigest());
		}

		if (request != null && request.getFocusModeEnabled() != null) {
			user.setFocusModeEnabled(request.getFocusModeEnabled());
		}

		userRepository.save(user);
		return toAuthResponse(user, null);
	}

	public void deleteMe(String email) {
		User user = userRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new IllegalArgumentException("User not found"));

		String tombstone = "deleted-" + user.getId() + "-" + OffsetDateTime.now().toEpochSecond() + "@local";
		user.setName("Deleted User");
		user.setEmail(tombstone);
		user.setPasswordHash(passwordEncoder.encode("deleted-account"));
		userRepository.save(user);
	}

	private String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email is required");
		}
		return email.trim().toLowerCase();
	}

	private boolean isLegacySha256Match(String rawPassword, String storedHash) {
		return sha256(rawPassword).equalsIgnoreCase(storedHash);
	}

	private AuthResponse toAuthResponse(User user, String token) {
		return new AuthResponse(
			token,
			"Bearer",
			user.getName(),
			user.getEmail(),
			user.getHeadline(),
			user.getBio(),
			user.getLocation(),
			user.getAccentColor(),
			user.isProfilePublic(),
			user.isEmailNotifications(),
			user.isWorkspaceDigest(),
			user.isFocusModeEnabled()
		);
	}

	private String trimToNull(String value, int maxLength) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty()) {
			return null;
		}
		return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
	}

	private String sha256(String raw) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to verify legacy password hash", ex);
		}
	}
}
