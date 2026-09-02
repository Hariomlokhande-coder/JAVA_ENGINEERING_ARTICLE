package com.technicalblog.service;

import com.technicalblog.config.StorageProperties;
import com.technicalblog.dto.response.FileUploadResponse;
import com.technicalblog.entity.StoredImage;
import com.technicalblog.exception.FileStorageException;
import com.technicalblog.exception.InvalidRequestException;
import com.technicalblog.exception.ResourceNotFoundException;
import com.technicalblog.repository.StoredImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Stores article images in the database and returns the public URL.
 * The original file name is never used, only its format decides the extension,
 * so a crafted name has nothing to attack.
 */
@Service
public class FileStorageService {

    private static final Map<String, String> EXTENSION_BY_TYPE = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/gif", ".gif",
            "image/webp", ".webp");

    /** Leading bytes every accepted format must start with, checked against the declared type. */
    private static final Map<String, byte[]> SIGNATURE_BY_TYPE = Map.of(
            "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G'},
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/gif", new byte[]{'G', 'I', 'F', '8'},
            "image/webp", new byte[]{'R', 'I', 'F', 'F'});

    private static final int SIGNATURE_LENGTH = 4;

    private final StorageProperties properties;
    private final StoredImageRepository images;

    public FileStorageService(StorageProperties properties, StoredImageRepository images) {
        this.properties = properties;
        this.images = images;
    }

    @Transactional
    public FileUploadResponse store(MultipartFile file) {
        validate(file);

        String contentType = file.getContentType().toLowerCase(Locale.ENGLISH);
        String fileName = UUID.randomUUID() + EXTENSION_BY_TYPE.get(contentType);

        byte[] bytes;
        try (InputStream inputStream = file.getInputStream()) {
            bytes = inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new FileStorageException("Could not read the uploaded file", ex);
        }

        images.save(StoredImage.builder()
                .filename(fileName)
                .contentType(contentType)
                .sizeBytes(bytes.length)
                .data(bytes)
                .build());

        String url = "/uploads/" + properties.articlesFolder() + "/" + fileName;
        return new FileUploadResponse(url, fileName, bytes.length);
    }

    @Transactional(readOnly = true)
    public StoredImage load(String fileName) {
        return images.findByFilename(fileName)
                .orElseThrow(() -> ResourceNotFoundException.of("Image", fileName));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Please choose a file to upload");
        }
        if (file.getSize() > properties.maxFileSizeBytes()) {
            throw new InvalidRequestException(
                    "The file is larger than the " + (properties.maxFileSizeBytes() / (1024 * 1024)) + " MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !properties.allowedContentTypes().contains(contentType.toLowerCase(Locale.ENGLISH))
                || !EXTENSION_BY_TYPE.containsKey(contentType.toLowerCase(Locale.ENGLISH))) {
            throw new InvalidRequestException("Only PNG, JPEG, GIF and WEBP images can be uploaded");
        }
        verifySignature(file, contentType.toLowerCase(Locale.ENGLISH));
    }

    /**
     * The declared content type is attacker controlled, so the real bytes decide.
     * This stops a script or an HTML page from being stored behind an image name.
     */
    private void verifySignature(MultipartFile file, String contentType) {
        byte[] expected = SIGNATURE_BY_TYPE.get(contentType);
        byte[] actual = new byte[SIGNATURE_LENGTH];

        try (InputStream inputStream = file.getInputStream()) {
            if (inputStream.readNBytes(actual, 0, SIGNATURE_LENGTH) < expected.length) {
                throw new InvalidRequestException("That file is not a readable image");
            }
        } catch (IOException ex) {
            throw new FileStorageException("Could not read the uploaded file", ex);
        }

        for (int index = 0; index < expected.length; index++) {
            if (actual[index] != expected[index]) {
                throw new InvalidRequestException("The file content does not match a " + contentType + " image");
            }
        }
    }
}
