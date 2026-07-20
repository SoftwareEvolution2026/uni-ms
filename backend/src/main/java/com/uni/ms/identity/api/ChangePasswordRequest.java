package com.uni.ms.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 200) String currentPassword,
        @NotBlank @Size(min = 8, max = 72,
                message = "Password must be between 8 and 72 characters")
        String newPassword
) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=[REDACTED], newPassword=[REDACTED]]";
    }
}
