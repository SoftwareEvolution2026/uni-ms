package com.uni.ms.common.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(new JwtProperties(
            "unit-test-secret-that-is-definitely-long-enough-256", 900000, 604800000));

    @Test
    void generatesAndValidatesToken() {
        String token = jwtService.generateAccessToken("alice@uni.ms", List.of("ROLE_STUDENT"));

        assertTrue(jwtService.isValid(token));
        assertEquals("alice@uni.ms", jwtService.extractEmail(token));
        assertEquals(List.of("ROLE_STUDENT"), jwtService.extractRoles(token));
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateAccessToken("bob@uni.ms", List.of("ROLE_ADMIN"));
        assertFalse(jwtService.isValid(token + "tampered"));
    }

    @Test
    void rejectsGarbage() {
        assertFalse(jwtService.isValid("not-a-jwt"));
    }
}
