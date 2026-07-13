package com.uni.ms.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the full Team 1 flow end-to-end against an in-memory H2 database:
 * admin logs in -> admin creates a user with a role -> that user logs in
 * -> refresh rotates -> the old refresh token is rejected.
 * The seeded admin comes from DataInitializer, which runs in the test context too.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    private String login(String email, String password) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void seededAdminCanLogIn() throws Exception {
        String body = """
                {"email":"admin@uni.ms","password":"Admin123!"}""";
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.user.roles[0]").value("ROLE_ADMIN"));
    }

    @Test
    void adminCreatesUserThenThatUserLogsInAndRefreshes() throws Exception {
        String adminToken = login("admin@uni.ms", "Admin123!");

        // 1. Admin creates a lecturer
        String createBody = """
                {"fullName":"Jane Doe","email":"jane@uni.ms","password":"Passw0rd!",
                 "roles":["ROLE_LECTURER"]}""";
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("jane@uni.ms"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_LECTURER"));

        // 2. The new user logs in
        String loginBody = """
                {"email":"jane@uni.ms","password":"Passw0rd!"}""";
        String loginResp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = mapper.readTree(loginResp);
        String accessToken = json.get("accessToken").asText();
        String refreshToken = json.get("refreshToken").asText();

        // 3. Access a protected endpoint
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@uni.ms"));

        // 4. Refresh rotates the token; the old one is then revoked
        String refreshBody = "{\"refreshToken\":\"" + refreshToken + "\"}";
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminCannotCreateUsers() throws Exception {
        String studentToken = login("student@uni.ms", "Student123!");
        String createBody = """
                {"fullName":"Hacker","email":"hacker@uni.ms","password":"Passw0rd!",
                 "roles":["ROLE_ADMIN"]}""";
        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpointRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void adminCanDeleteUserButNotThemselves() throws Exception {
        String adminToken = login("admin@uni.ms", "Admin123!");

        String meResp = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long adminId = mapper.readTree(meResp).get("id").asLong();

        mockMvc.perform(delete("/api/v1/users/" + adminId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        String createBody = """
                {"fullName":"Temp User","email":"temp@uni.ms","password":"Passw0rd!",
                 "roles":["ROLE_STUDENT"]}""";
        String created = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long tempId = mapper.readTree(created).get("id").asLong();

        mockMvc.perform(delete("/api/v1/users/" + tempId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void removedRegisterEndpointReturns404NotServerError() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"x","email":"z@z.ms","password":"Passw0rd!"}"""))
                .andExpect(status().isNotFound());
    }
}
