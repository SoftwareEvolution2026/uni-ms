package com.uni.ms.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.ms.testsupport.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserEditIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    private String adminToken() throws Exception {
        String resp = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@uni.ms","password":"Admin123!"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(resp).get("accessToken").asText();
    }

    @Test
    void adminUpdatesUserDetailsAndRoles() throws Exception {
        String token = adminToken();

        String created = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Old Name","email":"edit-me@uni.ms",
                                 "password":"Passw0rd!","roles":["ROLE_ACADEMIC_MANAGER"]}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();

        mockMvc.perform(put("/api/v1/users/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"New Name","email":"edit-me@uni.ms",
                                 "roles":["ROLE_ADMIN","ROLE_ACADEMIC_MANAGER"],"enabled":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"))
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.containsInAnyOrder(
                        "ROLE_ADMIN", "ROLE_ACADEMIC_MANAGER")));
    }

    @Test
    void adminCannotRemoveOwnAdminRole() throws Exception {
        String token = adminToken();

        String me = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@uni.ms","password":"Admin123!"}"""))
                .andReturn().getResponse().getContentAsString();
        long adminId = mapper.readTree(me).get("user").get("id").asLong();

        mockMvc.perform(put("/api/v1/users/" + adminId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"System Admin","email":"admin@uni.ms",
                                 "roles":["ROLE_ACADEMIC_MANAGER"],"enabled":true}"""))
                .andExpect(status().isBadRequest());
    }
}
