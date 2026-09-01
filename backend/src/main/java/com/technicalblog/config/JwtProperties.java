package com.technicalblog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the app.jwt.* settings. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationMs,
        String issuer
) {
}
