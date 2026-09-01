package com.technicalblog.dto.response;

import java.util.List;

/** A category together with the articles it contains (roadmap and category page). */
public record CategoryDetailResponse(
        Long id,
        String name,
        String slug,
        String description,
        Integer displayOrder,
        List<ArticleSummaryResponse> articles
) {
}
