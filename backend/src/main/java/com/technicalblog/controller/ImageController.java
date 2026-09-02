package com.technicalblog.controller;

import com.technicalblog.entity.StoredImage;
import com.technicalblog.service.FileStorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Serves an uploaded image out of the database.
 * The URL keeps the /uploads/... shape the stored article content already uses,
 * so moving the bytes into the database changed nothing for existing articles.
 */
@RestController
@RequestMapping("/uploads")
public class ImageController {

    private final FileStorageService fileStorageService;

    public ImageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    // The folder segment is part of the historic URL shape only, the name alone is unique.
    @GetMapping("/*/{fileName}")
    public ResponseEntity<byte[]> find(@PathVariable String fileName) {
        StoredImage image = fileStorageService.load(fileName);

        return ResponseEntity.ok()
                // The name is a fresh UUID for every upload, so the bytes behind it never change.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .contentLength(image.getSizeBytes())
                .body(image.getData());
    }
}
