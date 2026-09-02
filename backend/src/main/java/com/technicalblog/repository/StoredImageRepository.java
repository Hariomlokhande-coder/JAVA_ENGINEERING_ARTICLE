package com.technicalblog.repository;

import com.technicalblog.entity.StoredImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredImageRepository extends JpaRepository<StoredImage, Long> {

    Optional<StoredImage> findByFilename(String filename);
}
