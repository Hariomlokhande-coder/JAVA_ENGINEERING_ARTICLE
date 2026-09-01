package com.technicalblog.dto.response;

import com.technicalblog.entity.Role;

import java.time.Instant;

public record LoginResponse(
        String token,
        String type,
        Role role,
        String username,
        String email,
        Instant expiresAt
) {
    public static LoginResponse bearer(String token, Role role, String username, String email, Instant expiresAt) {
        return new LoginResponse(token, "Bearer", role, username, email, expiresAt);
    }
}
