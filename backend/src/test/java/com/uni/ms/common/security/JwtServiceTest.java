package com.uni.ms.common.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET =
            "unit-test-secret-that-is-definitely-long-enough-for-hmac-sha256";

    private final JwtService jwtService = new JwtService(new JwtProperties(
            SECRET, 900000, 604800000, "uni-ms-test", "uni-ms-api", 0));

    @Test
    void tokenContainsStableIdentityAndSecurityClaims() {
        String token = jwtService.generateAccessToken(42L, List.of("ROLE_ADMIN"));

        JwtService.AccessTokenClaims claims = jwtService.parseAccessToken(token);

        assertEquals(42L, claims.userId());
        assertEquals(List.of("ROLE_ADMIN"), claims.authorities());
        assertNotNull(claims.tokenId());
        assertNotNull(claims.expiresAt());
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(7L, List.of("ROLE_ACADEMIC_MANAGER"));
        assertThrows(Exception.class, () -> jwtService.parseAccessToken(token + "tampered"));
    }

    @Test
    void rejectsTokenWithWrongAudience() {
        JwtService otherAudience = new JwtService(new JwtProperties(
                SECRET, 900000, 604800000, "uni-ms-test", "another-api", 0));
        String token = jwtService.generateAccessToken(7L, List.of("ROLE_ADMIN"));
        assertThrows(Exception.class, () -> otherAudience.parseAccessToken(token));
    }
}
