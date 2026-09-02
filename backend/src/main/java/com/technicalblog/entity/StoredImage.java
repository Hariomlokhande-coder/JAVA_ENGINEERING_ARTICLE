package com.technicalblog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * An uploaded article image, bytes and all.
 * Keeping the image in the database rather than on disk means it travels with the
 * data: any machine pointed at this database sees the same pictures, and a database
 * backup is a complete backup.
 */
@Entity
@Table(name = "stored_images",
        uniqueConstraints = @UniqueConstraint(name = "uk_stored_images_filename", columnNames = "filename"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredImage {

    /** Matches app.storage.max-file-size-bytes, and keeps the column from defaulting to 255 bytes. */
    public static final int MAX_BYTES = 5 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The generated UUID name that appears in the public URL. */
    @Column(nullable = false, length = 80)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = MAX_BYTES)
    private byte[] data;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
