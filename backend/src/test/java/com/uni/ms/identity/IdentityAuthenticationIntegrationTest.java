package com.uni.ms.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.ms.common.audit.AuditLogRepository;
import com.uni.ms.identity.domain.Role;
import com.uni.ms.identity.domain.User;
import com.uni.ms.identity.infrastructure.UserRepository;
import com.uni.ms.testsupport.PostgreSqlIntegrationTest;
import com.uni.ms.testsupport.SecurityProbeController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityProbeController.class)
class IdentityAuthenticationIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void successfulLoginReturnsFinalContractWithoutPasswordHash() throws Exception {
        JsonNode response = login("  ADMIN@UNIVERSITY.TEST ", "Admin123!", 200);

        assertEquals("Bearer", response.get("tokenType").asText());
        assertEquals(900, response.get("expiresIn").asLong());
        assertEquals("ADMIN", response.at("/user/role").asText());
        assertEquals("admin@university.test", response.at("/user/email").asText());
        assertTrue(response.at("/user/passwordHash").isMissingNode());
    }

    @Test
    void invalidPasswordAndUnknownEmailHaveSameExternalProblem() throws Exception {
        JsonNode wrongPassword = login("admin@university.test", "Wrong123!", 401);
        JsonNode unknown = login(uniqueEmail(), "Wrong123!", 401);

        assertEquals("INVALID_CREDENTIALS", wrongPassword.get("code").asText());
        assertEquals(wrongPassword.get("code"), unknown.get("code"));
        assertEquals(wrongPassword.get("detail"), unknown.get("detail"));
    }

    @Test
    void disabledUserIsRejected() throws Exception {
        User user = createUser(uniqueEmail(), "Password123!", Role.ACADEMIC_MANAGER, false);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(user.getEmail(), "Password123!")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_DISABLED"));
    }

    @Test
    void rateLimitAppliesAfterFiveFailures() throws Exception {
        String email = uniqueEmail();
        for (int attempt = 0; attempt < 5; attempt++) {
            login(email, "Wrong123!", 401);
        }
        login(email, "Wrong123!", 429);
    }

    @Test
    void successfulLoginResetsRateLimitCounter() throws Exception {
        User user = createUser(uniqueEmail(), "Password123!", Role.ACADEMIC_MANAGER, true);
        for (int attempt = 0; attempt < 4; attempt++) {
            login(user.getEmail(), "Wrong123!", 401);
        }
        login(user.getEmail(), "Password123!", 200);
        for (int attempt = 0; attempt < 4; attempt++) {
            login(user.getEmail(), "Wrong123!", 401);
        }
    }

    @Test
    void refreshRotatesTokenAndReuseRevokesTheFamily() throws Exception {
        User user = createUser(uniqueEmail(), "Password123!", Role.ACADEMIC_MANAGER, true);
        JsonNode firstLogin = login(user.getEmail(), "Password123!", 200);
        String original = firstLogin.get("refreshToken").asText();

        JsonNode refreshed = refresh(original, 200);
        String replacement = refreshed.get("refreshToken").asText();
        assertNotEquals(original, replacement);

        JsonNode reuse = refresh(original, 401);
        assertEquals("REFRESH_TOKEN_REUSED", reuse.get("code").asText());
        assertEquals("REFRESH_TOKEN_INVALID", refresh(replacement, 401).get("code").asText());
    }

    @Test
    void concurrentRefreshAllowsOnlyOneRotationAndDetectsReuse() throws Exception {
        User user = createUser(uniqueEmail(), "Password123!", Role.ACADEMIC_MANAGER, true);
        String refreshToken = login(user.getEmail(), "Password123!", 200)
                .get("refreshToken").asText();
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(() -> refreshStatus(refreshToken, start));
            Future<Integer> second = executor.submit(() -> refreshStatus(refreshToken, start));
            start.countDown();
            int firstStatus = first.get();
            int secondStatus = second.get();
            assertEquals(200, Math.min(firstStatus, secondStatus));
            assertEquals(401, Math.max(firstStatus, secondStatus));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void logoutIsIdempotentAndRevokesRefreshToken() throws Exception {
        User user = createUser(uniqueEmail(), "Password123!", Role.ACADEMIC_MANAGER, true);
        String token = login(user.getEmail(), "Password123!", 200)
                .get("refreshToken").asText();
        logout(token);
        logout(token);
        assertEquals("REFRESH_TOKEN_INVALID", refresh(token, 401).get("code").asText());
    }

    @Test
    void currentUserRequiresTokenAndNeverReturnsPasswordHash() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));

        JsonNode login = login("admin@university.test", "Admin123!", 200);
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + login.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@university.test"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void expiredAndInvalidAccessTokensUseStableSecurityCodes() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredAccessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void insufficientRoleReturnsNormalizedForbiddenProblem() throws Exception {
        User manager = createUser(uniqueEmail(), "Password123!", Role.ACADEMIC_MANAGER, true);
        String accessToken = login(manager.getEmail(), "Password123!", 200)
                .get("accessToken").asText();
        mockMvc.perform(get("/test/admin-only")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void passwordChangeRevokesRefreshTokensAndRequiresCurrentPassword() throws Exception {
        User user = createUser(uniqueEmail(), "Password123!", Role.ACADEMIC_MANAGER, true);
        JsonNode login = login(user.getEmail(), "Password123!", 200);
        String access = login.get("accessToken").asText();
        String refresh = login.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("Wrong123!", "NewPassword123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordBody("Password123!", "NewPassword123!")))
                .andExpect(status().isNoContent());

        refresh(refresh, 401);
        login(user.getEmail(), "Password123!", 401);
        login(user.getEmail(), "NewPassword123!", 200);
    }

    @Test
    void laravelTwoYBcryptHashesAreAccepted() throws Exception {
        String hash = passwordEncoder.encode("LaravelPassword123!").replaceFirst("^\\$2a\\$", "\\$2y\\$");
        User user = createUserWithHash(uniqueEmail(), hash, Role.ACADEMIC_MANAGER, true);
        login(user.getEmail(), "LaravelPassword123!", 200);
    }

    @Test
    void validationUsesNormalizedProblemFormat() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void expectedAuditEventsAreRecorded() throws Exception {
        User user = createUser(uniqueEmail(), "Password123!", Role.ACADEMIC_MANAGER, true);
        JsonNode login = login(user.getEmail(), "Password123!", 200);
        String refresh = login.get("refreshToken").asText();
        String rotated = refresh(refresh, 200).get("refreshToken").asText();
        refresh(refresh, 401);
        logout(rotated);

        var actions = auditLogRepository.findAll().stream().map(log -> log.getAction()).toList();
        assertTrue(actions.contains("LOGIN_SUCCESS"));
        assertTrue(actions.contains("REFRESH_SUCCESS"));
        assertTrue(actions.contains("REFRESH_TOKEN_REUSE"));
        assertTrue(actions.contains("LOGOUT"));
    }

    private User createUser(String email, String password, Role role, boolean enabled) {
        return createUserWithHash(email, passwordEncoder.encode(password), role, enabled);
    }

    private User createUserWithHash(String email, String hash, Role role, boolean enabled) {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(hash);
        user.setEnabled(enabled);
        user.setEmailVerified(true);
        user.setRole(role);
        return userRepository.save(user);
    }

    private JsonNode login(String email, String password, int expectedStatus) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode refresh(String refreshToken, int expectedStatus) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(refreshToken)))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private int refreshStatus(String refreshToken, CountDownLatch start) throws Exception {
        start.await();
        return mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(refreshToken)))
                .andReturn().getResponse().getStatus();
    }

    private void logout(String refreshToken) throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenBody(refreshToken)))
                .andExpect(status().isNoContent());
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@university.test";
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginPayload(email, password));
    }

    private String tokenBody(String token) throws Exception {
        return objectMapper.writeValueAsString(new TokenPayload(token));
    }

    private String passwordBody(String currentPassword, String newPassword) throws Exception {
        return objectMapper.writeValueAsString(
                new PasswordPayload(currentPassword, newPassword));
    }

    private String expiredAccessToken() {
        var key = Keys.hmacShaKeyFor(
                "test-only-secret-that-is-long-enough-for-hmac-sha256-signing".getBytes());
        Date now = new Date();
        return Jwts.builder()
                .issuer("uni-ms-test")
                .subject("1")
                .audience().add("uni-ms-test-api").and()
                .id(UUID.randomUUID().toString())
                .claim("authorities", java.util.List.of("ROLE_ADMIN"))
                .issuedAt(new Date(now.getTime() - 120_000))
                .expiration(new Date(now.getTime() - 60_000))
                .signWith(key)
                .compact();
    }

    private record LoginPayload(String email, String password) {
    }

    private record TokenPayload(String refreshToken) {
    }

    private record PasswordPayload(String currentPassword, String newPassword) {
    }
}
