package com.collab.workspace.service;

import com.collab.workspace.config.JwtUtil;
import com.collab.workspace.dto.AuthResponse;
import com.collab.workspace.dto.LoginRequest;
import com.collab.workspace.dto.SignupRequest;
import com.collab.workspace.entity.User;
import com.collab.workspace.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;

	public AuthService(UserRepository userRepository, JwtUtil jwtUtil) {
		this.userRepository = userRepository;
		this.jwtUtil = jwtUtil;
	}

	public AuthResponse signup(SignupRequest request) {
		String email = normalizeEmail(request.getEmail());
		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("User already exists with this email");
		}

		User user = new User();
		user.setName(request.getName().trim());
		user.setEmail(email);
		user.setPasswordHash(hash(request.getPassword()));
		user.setCreatedAt(Instant.now());
		userRepository.save(user);

		String token = jwtUtil.generateToken(email);
		return new AuthResponse(token, "Bearer", user.getName(), user.getEmail());
	}

	public AuthResponse login(LoginRequest request) {
		String email = normalizeEmail(request.getEmail());
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

		if (!user.getPasswordHash().equals(hash(request.getPassword()))) {
			throw new IllegalArgumentException("Invalid email or password");
		}

		String token = jwtUtil.generateToken(email);
		return new AuthResponse(token, "Bearer", user.getName(), user.getEmail());
	}

	public AuthResponse me(String email) {
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new IllegalArgumentException("User not found"));
		return new AuthResponse(null, "Bearer", user.getName(), user.getEmail());
	}

	private String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email is required");
		}
		return email.trim().toLowerCase();
	}

	private String hash(String raw) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to hash password", ex);
		}
	}
}
