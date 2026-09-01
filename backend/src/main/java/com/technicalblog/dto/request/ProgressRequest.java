package com.technicalblog.dto.request;

/** Partial update: a null field leaves that flag unchanged. */
public record ProgressRequest(
        Boolean completed,
        Boolean favourite
) {
}
