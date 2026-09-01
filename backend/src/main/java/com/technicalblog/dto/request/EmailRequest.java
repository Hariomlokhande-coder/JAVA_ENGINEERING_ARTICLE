package com.technicalblog.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Used by the resend verification and forgot password endpoints. */
public record EmailRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email is not valid")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email
) {
}
