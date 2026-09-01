package com.technicalblog.dto.response;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        Integer displayOrder,
        long articleCount,
        Instant createdAt,
        Instant updatedAt
) {
}
