package com.collab.workspace.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;

@Component
public class JwtSecretStartupValidator implements ApplicationRunner {

    private static final String DEFAULT_SECRET = "change-this-secret-for-prod";

    private final Environment environment;
    private final String jwtSecret;

    public JwtSecretStartupValidator(
        Environment environment,
        @Value("${app.jwt.secret:change-this-secret-for-prod}") String jwtSecret
    ) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProductionProfileActive()) {
            return;
        }

        String secret = jwtSecret == null ? "" : jwtSecret.trim();
        if (secret.isBlank() || DEFAULT_SECRET.equals(secret) || secret.length() < 32) {
            throw new IllegalStateException(
                "Unsafe app.jwt.secret for production. Set APP_JWT_SECRET to a strong value (at least 32 chars)."
            );
        }
    }

    private boolean isProductionProfileActive() {
        Set<String> prodProfiles = Set.of("prod", "production");
        return Arrays.stream(environment.getActiveProfiles())
            .map(String::toLowerCase)
            .anyMatch(prodProfiles::contains);
    }
}
