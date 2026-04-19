package com.collab.workspace.service;

import com.collab.workspace.config.JwtUtil;
import com.collab.workspace.dto.ApiMessageResponse;
import com.collab.workspace.dto.ForgotPasswordRequest;
import com.collab.workspace.dto.AuthResponse;
import com.collab.workspace.dto.LoginRequest;
import com.collab.workspace.dto.ResetPasswordRequest;
import com.collab.workspace.dto.SignupRequest;
import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.entity.User;
import com.collab.workspace.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.net.URLEncoder;

@Service
public class AuthService {
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private static final DateTimeFormatter EMAIL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final String DEFAULT_FRONTEND_BASE_URL = "https://collaborative-java-workspace-frontend.onrender.com";

	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
	private final JavaMailSender mailSender;
	private final String frontendBaseUrl;
	private final long passwordResetExpirationMinutes;

	public AuthService(
		UserRepository userRepository,
		JwtUtil jwtUtil,
		PasswordEncoder passwordEncoder,
		JavaMailSender mailSender,
		@Value("${app.frontend.base-url:https://collaborative-java-workspace-frontend.onrender.com}") String frontendBaseUrl,
		@Value("${app.password-reset.expiration-minutes:60}") long passwordResetExpirationMinutes
	) {
		this.userRepository = userRepository;
		this.jwtUtil = jwtUtil;
		this.passwordEncoder = passwordEncoder;
		this.mailSender = mailSender;
		this.frontendBaseUrl = frontendBaseUrl;
		this.passwordResetExpirationMinutes = passwordResetExpirationMinutes;
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

	@Transactional
	public ApiMessageResponse requestPasswordReset(ForgotPasswordRequest request) {
		String email = normalizeEmail(request.getEmail());
		User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
		if (user == null) {
			return new ApiMessageResponse("If the email exists, a reset link has been sent.");
		}

		String token = generatePasswordResetToken();
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(passwordResetExpirationMinutes);
		user.setPasswordResetTokenHash(sha256(token));
		user.setPasswordResetTokenExpiresAt(expiresAt);
		userRepository.save(user);
		sendPasswordResetEmail(user, token, expiresAt);
		return new ApiMessageResponse("If the email exists, a reset link has been sent.");
	}

	@Transactional
	public ApiMessageResponse resetPassword(ResetPasswordRequest request) {
		String tokenHash = sha256(request.getToken().trim());
		User user = userRepository.findByPasswordResetTokenHash(tokenHash)
			.orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset link"));

		LocalDateTime expiresAt = user.getPasswordResetTokenExpiresAt();
		if (expiresAt == null || expiresAt.isBefore(LocalDateTime.now())) {
			user.setPasswordResetTokenHash(null);
			user.setPasswordResetTokenExpiresAt(null);
			userRepository.save(user);
			throw new IllegalArgumentException("Invalid or expired password reset link");
		}

		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
		user.setPasswordResetTokenHash(null);
		user.setPasswordResetTokenExpiresAt(null);
		userRepository.save(user);
		return new ApiMessageResponse("Password updated successfully.");
	}

	private String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("Email is required");
		}
		return email.trim().toLowerCase();
	}

	private String generatePasswordResetToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private void sendPasswordResetEmail(User user, String token, LocalDateTime expiresAt) {
		String resetLink = buildPasswordResetLink(token);
		String subject = "Reset your password";
		String expirationText = expiresAt.format(EMAIL_TIME_FORMATTER);
		String html = """
			<p>Hello %s,</p>
			<p>We received a request to reset your password for the Collaborative Java Workspace account linked to <strong>%s</strong>.</p>
			<p><a href="%s">Reset your password</a></p>
			<p>If the button does not open, copy and paste this link in your browser:</p>
			<p><a href="%s">%s</a></p>
			<p>This link expires at %s.</p>
			<p>If you did not request this, you can ignore this email.</p>
			""".formatted(escapeHtml(user.getName()), escapeHtml(user.getEmail()), resetLink, resetLink, resetLink, expirationText);

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
			helper.setTo(user.getEmail());
			helper.setSubject(subject);
			helper.setText(html, true);
			mailSender.send(message);
		} catch (MessagingException ex) {
			throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Unable to send password reset email", ex);
		}
	}

	private String buildPasswordResetLink(String token) {
		String baseUrl = normalizeFrontendBaseUrl(frontendBaseUrl);
		return baseUrl + "/reset-password?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
	}

	private String normalizeFrontendBaseUrl(String raw) {
		String baseUrl = raw == null ? "" : raw.trim();
		if (baseUrl.isEmpty()) {
			baseUrl = DEFAULT_FRONTEND_BASE_URL;
		}
		if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
			baseUrl = "https://" + baseUrl;
		}
		while (baseUrl.endsWith("/")) {
			baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
		}
		return baseUrl;
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
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
