package com.collab.workspace.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class SecurityConfig {

	@Bean
	public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter jwtFilter) {
		FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(jwtFilter);
		registration.addUrlPatterns("/api/*");
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}
}
