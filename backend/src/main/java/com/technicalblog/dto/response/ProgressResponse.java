package com.technicalblog.dto.response;

public record ProgressResponse(
        Long articleId,
        boolean completed,
        boolean favourite
) {
}
