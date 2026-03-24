package com.collab.workspace.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

	private static final long DEFAULT_EXPIRY_SECONDS = 24 * 60 * 60;

	private final SecretKey secretKey;
	private final long expirySeconds;

	public JwtUtil(
		@Value("${app.jwt.secret:change-this-secret-for-prod}") String secret,
		@Value("${app.jwt.expiration-seconds:" + DEFAULT_EXPIRY_SECONDS + "}") long expirySeconds
	) {
		this.secretKey = toSigningKey(secret);
		this.expirySeconds = expirySeconds;
	}

	public String generateToken(String email) {
		Instant now = Instant.now();
		return Jwts.builder()
			.subject(email)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusSeconds(expirySeconds)))
			.signWith(secretKey)
			.compact();
	}

	public String extractEmail(String token) {
		Claims claims = parseClaims(token);
		return claims != null ? claims.getSubject() : null;
	}

	public boolean isValid(String token) {
		return parseClaims(token) != null;
	}

	private Claims parseClaims(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}
		try {
			return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		} catch (JwtException | IllegalArgumentException ex) {
			return null;
		}
	}

	private SecretKey toSigningKey(String secret) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] keyBytes = digest.digest(secret.getBytes(StandardCharsets.UTF_8));
			return Keys.hmacShaKeyFor(keyBytes);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to initialize JWT signing key", ex);
		}
	}
}
