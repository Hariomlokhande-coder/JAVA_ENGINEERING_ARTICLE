package com.technicalblog.dto.response;

import com.technicalblog.entity.Role;

public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        Role role
) {
}
