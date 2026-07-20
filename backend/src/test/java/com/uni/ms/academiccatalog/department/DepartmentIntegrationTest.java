package com.uni.ms.academiccatalog.department;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.ms.academiccatalog.department.api.CreateDepartmentRequest;
import com.uni.ms.academiccatalog.department.api.UpdateDepartmentRequest;
import com.uni.ms.academiccatalog.department.application.DepartmentApplicationService;
import com.uni.ms.academiccatalog.department.domain.DepartmentStatus;
import com.uni.ms.academiccatalog.department.infrastructure.DepartmentRepository;
import com.uni.ms.common.audit.AuditLogRepository;
import com.uni.ms.common.exception.ErrorCode;
import com.uni.ms.common.exception.ProblemException;
import com.uni.ms.testsupport.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DepartmentIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DepartmentApplicationService departmentService;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void cleanDepartmentData() {
        jdbcTemplate.update("DELETE FROM courses");
        departmentRepository.deleteAll();
    }

    @Test
    void adminAndAcademicManagerCanCreateDepartments() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .with(user("1").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Computer Science", " cs ", "Science", null)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/departments/")))
                .andExpect(jsonPath("$.departmentCode").value("CS"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/departments")
                        .with(user("2").roles("ACADEMIC_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Mathematics", "MATH", "Science", "Numbers")))
                .andExpect(status().isCreated());
    }

    @Test
    void securityReturnsNormalizedUnauthorizedAndForbiddenProblems() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));

        JsonNode created = createThroughService("Physics", "PHY", "Science");
        long id = created.get("id").asLong();
        departmentService.softDelete(id, "userId:1");
        mockMvc.perform(delete("/api/v1/departments/{id}/permanent", id)
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void validationNotFoundDuplicateAndInvalidSortUseStableProblems() throws Exception {
        mockMvc.perform(post("/api/v1/departments")
                        .with(user("1").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("", "bad code", "", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.departmentName").exists())
                .andExpect(jsonPath("$.fieldErrors.departmentCode").exists())
                .andExpect(jsonPath("$.fieldErrors.faculty").exists());

        mockMvc.perform(get("/api/v1/departments/999999")
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_FOUND"));

        createThroughService("Computer Science", "CS", "Science");
        mockMvc.perform(post("/api/v1/departments")
                        .with(user("2").roles("ACADEMIC_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("Another", "cs", "Other", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_CODE_ALREADY_EXISTS"));

        mockMvc.perform(get("/api/v1/departments?sort=passwordHash,desc")
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));

        mockMvc.perform(get("/api/v1/departments?status=UNKNOWN")
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DEPARTMENT_STATUS"));
    }

    @Test
    void searchFiltersSortsAndPaginatesWithoutReturningDeletedRows() throws Exception {
        JsonNode cs = createThroughService("Computer Science", "CS", "Science");
        createThroughService("Applied Mathematics", "MATH", "Science");
        JsonNode law = createThroughService("Law", "LAW", "Humanities");
        long lawId = law.get("id").asLong();
        departmentService.update(lawId, new UpdateDepartmentRequest(
                "Law", "LAW", "Humanities", null, DepartmentStatus.INACTIVE,
                law.get("version").asLong()), "userId:1");
        departmentService.softDelete(cs.get("id").asLong(), "userId:1");

        mockMvc.perform(get("/api/v1/departments?search=math&faculty=science"
                        + "&page=0&size=1&sort=departmentCode,desc")
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].departmentCode").value("MATH"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.sort[0].property").value("departmentCode"))
                .andExpect(jsonPath("$.sort[0].direction").value("DESC"));

        mockMvc.perform(get("/api/v1/departments?status=INACTIVE")
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].departmentCode").value("LAW"));

        mockMvc.perform(get("/api/v1/departments/trash")
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].departmentCode").value("CS"))
                .andExpect(jsonPath("$.content[0].deletedAt").exists());
    }

    @Test
    void completeSoftDeleteRestoreAndPermanentDeleteLifecycleWorks() throws Exception {
        JsonNode created = createThroughService("Chemistry", "CHEM", "Science");
        long id = created.get("id").asLong();

        mockMvc.perform(delete("/api/v1/departments/{id}", id)
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/departments/{id}", id)
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_ALREADY_DELETED"));

        mockMvc.perform(get("/api/v1/departments/{id}", id)
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/departments/{id}/restore", id)
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedAt").doesNotExist())
                .andExpect(jsonPath("$.departmentCode").value("CHEM"));

        mockMvc.perform(post("/api/v1/departments/{id}/restore", id)
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_NOT_DELETED"));

        departmentService.softDelete(id, "userId:1");
        mockMvc.perform(delete("/api/v1/departments/{id}/permanent", id)
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isNoContent());
        assertTrue(departmentRepository.findById(id).isEmpty());

        var actions = auditLogRepository.findAll().stream().map(log -> log.getAction()).toList();
        assertTrue(actions.contains("DEPARTMENT_CREATED"));
        assertTrue(actions.contains("DEPARTMENT_DELETED"));
        assertTrue(actions.contains("DEPARTMENT_RESTORED"));
        assertTrue(actions.contains("DEPARTMENT_PERMANENTLY_DELETED"));
    }

    @Test
    void optimisticLockingRejectsStaleRequestVersion() throws Exception {
        JsonNode created = createThroughService("Geology", "GEO", "Science");
        long id = created.get("id").asLong();
        long version = created.get("version").asLong();
        departmentService.update(id, new UpdateDepartmentRequest(
                "Earth Science", "GEO", "Science", null,
                DepartmentStatus.ACTIVE, version), "userId:1");

        mockMvc.perform(put("/api/v1/departments/{id}", id)
                        .with(user("2").roles("ACADEMIC_MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Stale", "GEO", "Science", "ACTIVE", version)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_VERSION_CONFLICT"));
    }

    @Test
    void codeRemainsUniqueAfterSoftDeleteAndConcurrentInsertion() throws Exception {
        JsonNode created = createThroughService("History", "HIST", "Humanities");
        departmentService.softDelete(created.get("id").asLong(), "userId:1");
        ProblemException softDeletedDuplicate = org.junit.jupiter.api.Assertions.assertThrows(
                ProblemException.class,
                () -> departmentService.create(new CreateDepartmentRequest(
                        "New History", "hist", "Humanities", null, null), "userId:1"));
        assertEquals(ErrorCode.DEPARTMENT_CODE_ALREADY_EXISTS,
                softDeletedDuplicate.getCode());

        String code = "CON" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(() -> concurrentCreate(code, start));
            Future<String> second = executor.submit(() -> concurrentCreate(code, start));
            start.countDown();
            String firstResult = first.get();
            String secondResult = second.get();
            assertTrue((firstResult.equals("CREATED") && secondResult.equals("DUPLICATE"))
                    || (firstResult.equals("DUPLICATE") && secondResult.equals("CREATED")));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void courseReferenceProtectsPermanentDeletionAndCourseCountAvoidsEntityLeakage()
            throws Exception {
        JsonNode created = createThroughService("Engineering", "ENG", "Engineering");
        long id = created.get("id").asLong();
        insertCourse(id);

        mockMvc.perform(get("/api/v1/departments/{id}", id)
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseCount").value(1))
                .andExpect(jsonPath("$.hibernateLazyInitializer").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        departmentService.softDelete(id, "userId:1");
        mockMvc.perform(delete("/api/v1/departments/{id}/permanent", id)
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEPARTMENT_HAS_COURSES"));
    }

    @Test
    void OpenApiDocumentsEveryDepartmentEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/departments']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/departments/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/departments/trash']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/departments/{id}/restore']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/departments/{id}/permanent']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());
    }

    private JsonNode createThroughService(String name, String code, String faculty)
            throws Exception {
        var response = departmentService.create(new CreateDepartmentRequest(
                name, code, faculty, null, null), "userId:1");
        return objectMapper.valueToTree(response);
    }

    private String concurrentCreate(String code, CountDownLatch start) throws Exception {
        start.await();
        try {
            departmentService.create(new CreateDepartmentRequest(
                    "Concurrent", code, "Science", null, null), "userId:1");
            return "CREATED";
        } catch (ProblemException ex) {
            return ex.getCode() == ErrorCode.DEPARTMENT_CODE_ALREADY_EXISTS
                    ? "DUPLICATE" : ex.getCode().name();
        }
    }

    private void insertCourse(long departmentId) {
        jdbcTemplate.update("""
                INSERT INTO courses (
                    department_id, course_name, course_code, credit_units, semester,
                    academic_year, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """, departmentId, "Software Engineering", "SE-" + departmentId,
                3, "SEMESTER_1", "2026/2027", "ACTIVE");
    }

    private String createBody(String name, String code, String faculty, String description)
            throws Exception {
        return objectMapper.writeValueAsString(new CreatePayload(
                name, code, faculty, description, null));
    }

    private String updateBody(String name, String code, String faculty, String status,
                              long version) throws Exception {
        return objectMapper.writeValueAsString(new UpdatePayload(
                name, code, faculty, null, status, version));
    }

    private record CreatePayload(
            String departmentName,
            String departmentCode,
            String faculty,
            String description,
            String status
    ) {
    }

    private record UpdatePayload(
            String departmentName,
            String departmentCode,
            String faculty,
            String description,
            String status,
            long version
    ) {
    }
}
