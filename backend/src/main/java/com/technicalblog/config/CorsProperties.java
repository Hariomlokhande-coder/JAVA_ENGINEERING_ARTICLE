package com.technicalblog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Binds the app.cors.* settings. */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
