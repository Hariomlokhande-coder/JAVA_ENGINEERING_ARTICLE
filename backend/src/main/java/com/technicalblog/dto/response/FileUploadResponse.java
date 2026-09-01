package com.technicalblog.dto.response;

public record FileUploadResponse(
        String url,
        String fileName,
        long size
) {
}
