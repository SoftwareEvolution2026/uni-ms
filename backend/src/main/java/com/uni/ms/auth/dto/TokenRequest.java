package com.uni.ms.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Used by both /refresh and /logout — carries the opaque refresh token. */
public record TokenRequest(
        @NotBlank String refreshToken
) {
}
