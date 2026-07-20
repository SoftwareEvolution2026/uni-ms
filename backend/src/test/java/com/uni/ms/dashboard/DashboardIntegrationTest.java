package com.uni.ms.dashboard;

import com.uni.ms.testsupport.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void prepareStatistics() {
        jdbcTemplate.update("DELETE FROM courses");
        jdbcTemplate.update("DELETE FROM departments");
        jdbcTemplate.update("""
                INSERT INTO departments
                    (department_name, department_code, faculty, status, deleted_at)
                VALUES
                    ('Active', 'DA', 'Science', 'ACTIVE', NULL),
                    ('Inactive', 'DI', 'Science', 'INACTIVE', NULL),
                    ('Deleted', 'DD', 'Science', 'ACTIVE', CURRENT_TIMESTAMP)
                """);
        Long activeDepartment = jdbcTemplate.queryForObject(
                "SELECT id FROM departments WHERE department_code = 'DA'", Long.class);
        jdbcTemplate.update("""
                INSERT INTO courses
                    (department_id, course_name, course_code, credit_units, semester,
                     academic_year, status, deleted_at)
                VALUES
                    (?, 'Active Course', 'CA', 3, 'SEMESTER_1', '2026/2027', 'ACTIVE', NULL),
                    (?, 'Inactive Course', 'CI', 3, 'SEMESTER_2', '2026/2027', 'INACTIVE', NULL),
                    (?, 'Deleted Course', 'CD', 3, 'SUMMER', '2026/2027', 'ACTIVE',
                     CURRENT_TIMESTAMP)
                """, activeDepartment, activeDepartment, activeDepartment);
    }

    @Test
    void bothAuthorizedRolesReceiveOnlyNonDeletedStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard").with(user("1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDepartments").value(2))
                .andExpect(jsonPath("$.activeDepartments").value(1))
                .andExpect(jsonPath("$.inactiveDepartments").value(1))
                .andExpect(jsonPath("$.totalCourses").value(2))
                .andExpect(jsonPath("$.activeCourses").value(1))
                .andExpect(jsonPath("$.inactiveCourses").value(1));

        mockMvc.perform(get("/api/v1/dashboard")
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestUsesProblemDetails() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void OpenApiContainsDashboardEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/dashboard']").exists());
    }
}
