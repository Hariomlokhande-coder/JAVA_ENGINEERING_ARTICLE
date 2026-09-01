package com.technicalblog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must be at most 120 characters")
        String name,

        /** Optional: generated from the name when left empty. */
        @Size(max = 140, message = "Slug must be at most 140 characters")
        @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug may only contain lowercase letters, numbers and hyphens")
        String slug,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @PositiveOrZero(message = "Display order cannot be negative")
        Integer displayOrder
) {
}
