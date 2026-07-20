package com.uni.ms.identity.api;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    public static AuthResponse bearer(String accessToken, String refreshToken,
                                      long expiresIn, UserSummary user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }

    @Override
    public String toString() {
        return "AuthResponse[accessToken=[REDACTED], refreshToken=[REDACTED], tokenType="
                + tokenType + ", expiresIn=" + expiresIn + ", user=" + user + "]";
    }
}
