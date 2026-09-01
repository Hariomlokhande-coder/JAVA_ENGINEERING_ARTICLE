package com.technicalblog.dto.request;

import com.technicalblog.entity.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ArticleRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        /** Optional: generated from the title when left empty. */
        @Size(max = 240, message = "Slug must be at most 240 characters")
        @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug may only contain lowercase letters, numbers and hyphens")
        String slug,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotBlank(message = "Content is required")
        @Size(max = 100_000, message = "Content must be at most 100000 characters")
        String content,

        @NotNull(message = "Category is required")
        Long categoryId,

        @PositiveOrZero(message = "Display order cannot be negative")
        Integer displayOrder,

        @Size(max = 500, message = "GitHub URL must be at most 500 characters")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "GitHub URL must start with http:// or https://")
        String githubUrl,

        @Size(max = 500, message = "YouTube URL must be at most 500 characters")
        @Pattern(regexp = "^$|^https?://\\S+$", message = "YouTube URL must start with http:// or https://")
        String youtubeUrl,

        @Size(max = 500, message = "Thumbnail URL must be at most 500 characters")
        String thumbnailUrl,

        Boolean published,

        Difficulty difficulty,

        @Size(max = 15, message = "An article can have at most 15 tags")
        List<@Size(max = 50, message = "A tag must be at most 50 characters") String> tags
) {
}
