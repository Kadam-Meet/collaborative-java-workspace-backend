package com.collab.workspace.service;

import com.collab.workspace.config.JwtUtil;
import com.collab.workspace.dto.AuthResponse;
import com.collab.workspace.dto.LoginRequest;
import com.collab.workspace.dto.SignupRequest;
import com.collab.workspace.entity.User;
import com.collab.workspace.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
		user.setCreatedAt(LocalDateTime.now());
		userRepository.save(user);

		String token = jwtUtil.generateToken(email);
		return new AuthResponse(token, "Bearer", user.getName(), user.getEmail());
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
		return new AuthResponse(token, "Bearer", user.getName(), user.getEmail());
	}

	public AuthResponse me(String email) {
		User user = userRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new IllegalArgumentException("User not found"));
		return new AuthResponse(null, "Bearer", user.getName(), user.getEmail());
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
