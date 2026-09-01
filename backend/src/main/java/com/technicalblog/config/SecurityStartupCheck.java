package com.technicalblog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Refuses to serve traffic with development secrets outside the dev profile.
 * A deployment that forgets to set JWT_SECRET should fail loudly, not run insecurely.
 */
@Component
public class SecurityStartupCheck implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(SecurityStartupCheck.class);
    private static final String DEV_PROFILE = "dev";
    private static final String DEFAULT_SECRET = "change-this-development-secret-key-min-32-chars";
    private static final List<String> WEAK_PASSWORDS = List.of("Admin@12345", "admin", "password", "changeme");

    private final JwtProperties jwtProperties;
    private final AdminSeedProperties adminProperties;
    private final Environment environment;

    public SecurityStartupCheck(JwtProperties jwtProperties,
                                AdminSeedProperties adminProperties,
                                Environment environment) {
        this.jwtProperties = jwtProperties;
        this.adminProperties = adminProperties;
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        boolean development = Arrays.asList(environment.getActiveProfiles()).contains(DEV_PROFILE);

        if (DEFAULT_SECRET.equals(jwtProperties.secret())) {
            if (!development) {
                throw new IllegalStateException(
                        "JWT_SECRET is still the development default. Set a private value before running outside dev.");
            }
            log.warn("Running with the development JWT secret. Set JWT_SECRET before deploying.");
        }

        if (WEAK_PASSWORDS.contains(adminProperties.password())) {
            if (!development) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD is still a default value. Set a private password before running outside dev.");
            }
            log.warn("The admin account uses a default password. Change it before deploying.");
        }

        if (!development && adminProperties.seedEnabled()) {
            log.warn("Admin seeding is enabled outside dev. Set ADMIN_SEED_ENABLED=false once the account exists.");
        }
    }
}
