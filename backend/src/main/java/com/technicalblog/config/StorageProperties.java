package com.technicalblog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Binds the app.storage.* settings used by the upload endpoint. */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String articlesFolder,
        long maxFileSizeBytes,
        List<String> allowedContentTypes
) {
}
