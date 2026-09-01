package com.technicalblog.dto.response;

import com.technicalblog.entity.Difficulty;

import java.time.Instant;
import java.util.List;

public record ArticleResponse(
        Long id,
        String title,
        String slug,
        String description,
        String content,
        Integer displayOrder,
        String githubUrl,
        String youtubeUrl,
        String thumbnailUrl,
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
