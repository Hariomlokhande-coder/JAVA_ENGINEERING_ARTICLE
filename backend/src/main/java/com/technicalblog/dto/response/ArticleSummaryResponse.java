package com.technicalblog.dto.response;

import com.technicalblog.entity.Difficulty;

import java.time.Instant;
import java.util.List;

/** Lightweight article projection used by lists, cards and the roadmap. */
public record ArticleSummaryResponse(
        Long id,
        String title,
        String slug,
        String description,
        Integer displayOrder,
        String thumbnailUrl,
        String githubUrl,
        String youtubeUrl,
        boolean published,
        Difficulty difficulty,
        Long categoryId,
        String categoryName,
        String categorySlug,
        List<String> tags,
        Instant createdAt,
        Instant updatedAt
) {
}
