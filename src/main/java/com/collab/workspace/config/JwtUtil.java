package com.collab.workspace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtUtil {

	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final long DEFAULT_EXPIRY_SECONDS = 24 * 60 * 60;

	private final String secret;

	public JwtUtil(@Value("${app.jwt.secret:change-this-secret-for-prod}") String secret) {
		this.secret = secret;
	}

	public String generateToken(String email) {
		long expiresAt = Instant.now().getEpochSecond() + DEFAULT_EXPIRY_SECONDS;
		String payload = email + "|" + expiresAt;
		String signature = sign(payload);
		return encode(payload) + "." + encode(signature);
	}

	public String extractEmail(String token) {
		String payload = validateAndExtractPayload(token);
		if (payload == null) {
			return null;
		}
		String[] parts = payload.split("\\|", 2);
		return parts.length > 0 ? parts[0] : null;
	}

	public boolean isValid(String token) {
		return validateAndExtractPayload(token) != null;
	}

	private String validateAndExtractPayload(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}

		String[] tokenParts = token.split("\\.", 2);
		if (tokenParts.length != 2) {
			return null;
		}

		String payload = decode(tokenParts[0]);
		String signature = decode(tokenParts[1]);
		if (payload == null || signature == null) {
			return null;
		}

		String expectedSignature = sign(payload);
		if (!expectedSignature.equals(signature)) {
			return null;
		}

		String[] payloadParts = payload.split("\\|", 2);
		if (payloadParts.length != 2) {
			return null;
		}

		try {
			long expiresAt = Long.parseLong(payloadParts[1]);
			if (Instant.now().getEpochSecond() >= expiresAt) {
				return null;
			}
		} catch (NumberFormatException ex) {
			return null;
		}

		return payload;
	}

	private String sign(String value) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
			byte[] raw = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to generate token signature", ex);
		}
	}

	private String encode(String value) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
	}

	private String decode(String value) {
		try {
			byte[] raw = Base64.getUrlDecoder().decode(value);
			return new String(raw, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}
}
