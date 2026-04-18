package com.collab.workspace.controller;

import com.collab.workspace.dto.ApiMessageResponse;
import com.collab.workspace.dto.AuthResponse;
import com.collab.workspace.dto.ForgotPasswordRequest;
import com.collab.workspace.dto.LoginRequest;
import com.collab.workspace.dto.ResetPasswordRequest;
import com.collab.workspace.dto.SignupRequest;
import com.collab.workspace.dto.WorkspaceRequest;
import com.collab.workspace.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/signup")
	public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ResponseEntity.ok(authService.signup(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		return ResponseEntity.ok(authService.requestPasswordReset(request));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		return ResponseEntity.ok(authService.resetPassword(request));
	}

	@GetMapping("/me")
	public ResponseEntity<AuthResponse> me(jakarta.servlet.http.HttpServletRequest request) {
		Object email = request.getAttribute("authUserEmail");
		if (email == null) {
			return ResponseEntity.status(401).build();
		}
		return ResponseEntity.ok(authService.me(email.toString()));
	}

	@PutMapping("/me")
	public ResponseEntity<AuthResponse> updateMe(
		@RequestBody WorkspaceRequest request,
		jakarta.servlet.http.HttpServletRequest httpRequest
	) {
		Object email = httpRequest.getAttribute("authUserEmail");
		if (email == null) {
			return ResponseEntity.status(401).build();
		}
		return ResponseEntity.ok(authService.updateMe(email.toString(), request));
	}

	@DeleteMapping("/me")
	public ResponseEntity<Void> deleteMe(jakarta.servlet.http.HttpServletRequest httpRequest) {
		Object email = httpRequest.getAttribute("authUserEmail");
		if (email == null) {
			return ResponseEntity.status(401).build();
		}
		authService.deleteMe(email.toString());
		return ResponseEntity.noContent().build();
	}
}
