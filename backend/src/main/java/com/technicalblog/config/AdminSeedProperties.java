package com.technicalblog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the app.admin.* settings used to create the first ADMIN account. */
@ConfigurationProperties(prefix = "app.admin")
public record AdminSeedProperties(
        boolean seedEnabled,
        String username,
        String email,
        String password
) {
}
