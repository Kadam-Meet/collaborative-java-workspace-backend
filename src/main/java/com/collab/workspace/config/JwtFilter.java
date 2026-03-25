package com.collab.workspace.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class JwtFilter extends OncePerRequestFilter {

	private static final Set<String> PUBLIC_PATHS = Set.of(
		"/api/auth/login",
		"/api/auth/signup",
		"/api/v1/meta/health"
	);

	private final JwtUtil jwtUtil;

	public JwtFilter(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String path = request.getRequestURI();
		if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(path)) {
			filterChain.doFilter(request, response);
			return;
		}

		String header = request.getHeader("Authorization");
		String token = null;
		if (header != null && header.startsWith("Bearer ")) {
			token = header.substring(7);
		} else {
			String queryToken = request.getParameter("access_token");
			if (queryToken != null && !queryToken.isBlank()) {
				token = queryToken;
			}
		}

		if (token == null || token.isBlank()) {
			unauthorized(response, "Missing bearer token");
			return;
		}

		if (!jwtUtil.isValid(token)) {
			unauthorized(response, "Invalid or expired token");
			return;
		}

		String email = jwtUtil.extractEmail(token);
		if (email == null || email.isBlank()) {
			unauthorized(response, "Invalid token payload");
			return;
		}

		request.setAttribute("authUserEmail", email);
		filterChain.doFilter(request, response);
	}

	private boolean isPublic(String path) {
		return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
	}

	private void unauthorized(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		response.getWriter().write("{\"error\":\"" + message + "\"}");
	}
}
