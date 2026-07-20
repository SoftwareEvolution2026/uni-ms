package com.uni.ms.academiccatalog.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.ms.academiccatalog.course.api.CreateCourseRequest;
import com.uni.ms.academiccatalog.course.api.UpdateCourseRequest;
import com.uni.ms.academiccatalog.course.application.CourseApplicationService;
import com.uni.ms.academiccatalog.course.domain.CourseStatus;
import com.uni.ms.academiccatalog.course.domain.Semester;
import com.uni.ms.academiccatalog.course.infrastructure.CourseRepository;
import com.uni.ms.academiccatalog.department.api.CreateDepartmentRequest;
import com.uni.ms.academiccatalog.department.application.DepartmentApplicationService;
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
class CourseIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CourseApplicationService courseService;
    @Autowired
    private DepartmentApplicationService departmentService;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @BeforeEach
    void cleanCatalogData() {
        courseRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    @Test
    void bothRolesCanCreateAndReadCourses() throws Exception {
        long departmentId = createDepartment("Computer Science", "CS");

        mockMvc.perform(post("/api/v1/courses")
                        .with(user("1").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(departmentId, "Algorithms", " cs-301 ", null)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/courses/")))
                .andExpect(jsonPath("$.courseCode").value("CS-301"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/courses")
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void securityAndAdminOnlyPermanentDeletionAreEnforced() throws Exception {
        long departmentId = createDepartment("Science", "SCI");
        JsonNode course = createCourse(departmentId, "Physics", "PHY-101");
        long id = course.get("id").asLong();
        courseService.softDelete(id, "userId:1");

        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
        mockMvc.perform(delete("/api/v1/courses/{id}/permanent", id)
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void validationAndUnavailableDepartmentUseStableProblems() throws Exception {
        long departmentId = createDepartment("Engineering", "ENG");
        mockMvc.perform(post("/api/v1/courses")
                        .with(user("1").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(departmentId, "", "bad code", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.courseName").exists())
                .andExpect(jsonPath("$.fieldErrors.courseCode").exists());

        departmentService.softDelete(departmentId, "userId:1");
        mockMvc.perform(post("/api/v1/courses")
                        .with(user("1").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(departmentId, "Calculus", "CAL-101", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURSE_DEPARTMENT_UNAVAILABLE"));
    }

    @Test
    void searchFiltersSortsAndPaginates() throws Exception {
        long science = createDepartment("Science", "SCI");
        long business = createDepartment("Business", "BUS");
        createCourse(science, "Algorithms", "CS-301");
        JsonNode accounting = createCourse(business, "Accounting", "ACC-201");
        courseService.update(accounting.get("id").asLong(), new UpdateCourseRequest(
                business, "Accounting", "ACC-201", 4, Semester.SEMESTER_2,
                "2027/2028", "Finance", CourseStatus.INACTIVE,
                accounting.get("version").asLong()), "userId:1");

        mockMvc.perform(get("/api/v1/courses?search=account&departmentId={id}"
                                + "&semester=SEMESTER_2&academicYear=2027/2028"
                                + "&status=INACTIVE&page=0&size=1&sort=courseCode,desc", business)
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].courseCode").value("ACC-201"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.sort[0].direction").value("DESC"));
    }

    @Test
    void softDeleteRestoreAndPermanentDeleteLifecycleWorks() throws Exception {
        long departmentId = createDepartment("Humanities", "HUM");
        JsonNode created = createCourse(departmentId, "History", "HIS-101");
        long id = created.get("id").asLong();

        mockMvc.perform(delete("/api/v1/courses/{id}", id)
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/courses/trash")
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].deletedAt").exists());
        mockMvc.perform(post("/api/v1/courses/{id}/restore", id)
                        .with(user("2").roles("ACADEMIC_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());

        courseService.softDelete(id, "userId:1");
        mockMvc.perform(delete("/api/v1/courses/{id}/permanent", id)
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isNoContent());
        assertTrue(courseRepository.findById(id).isEmpty());
        assertTrue(auditLogRepository.findAll().stream()
                .map(log -> log.getAction())
                .anyMatch("COURSE_PERMANENTLY_DELETED"::equals));
    }

    @Test
    void optimisticLockDuplicateAndInvalidFiltersAreHandled() throws Exception {
        long departmentId = createDepartment("Law", "LAW");
        JsonNode created = createCourse(departmentId, "Contract Law", "LAW-201");
        long id = created.get("id").asLong();
        long version = created.get("version").asLong();
        courseService.update(id, updateRequest(departmentId, "Commercial Law", "LAW-201", version),
                "userId:1");

        mockMvc.perform(put("/api/v1/courses/{id}", id)
                        .with(user("1").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest(
                                departmentId, "Stale", "LAW-201", version))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURSE_VERSION_CONFLICT"));
        mockMvc.perform(post("/api/v1/courses")
                        .with(user("1").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(departmentId, "Duplicate", "law-201", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COURSE_CODE_ALREADY_EXISTS"));
        mockMvc.perform(get("/api/v1/courses?semester=THIRD&sort=secret,desc")
                        .with(user("1").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentCourseCodeInsertionIsDatabaseSafe() throws Exception {
        long departmentId = createDepartment("Medicine", "MED");
        String code = "MED-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = executor.submit(() -> concurrentCreate(departmentId, code, start));
            Future<String> second = executor.submit(() -> concurrentCreate(departmentId, code, start));
            start.countDown();
            assertEquals(1, java.util.stream.Stream.of(first.get(), second.get())
                    .filter("CREATED"::equals).count());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void OpenApiDocumentsEveryCourseEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/courses']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/courses/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/courses/trash']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/courses/{id}/restore']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/courses/{id}/permanent']").exists());
    }

    private long createDepartment(String name, String code) {
        return departmentService.create(new CreateDepartmentRequest(
                name, code, "Faculty", null, null), "userId:1").id();
    }

    private JsonNode createCourse(long departmentId, String name, String code) {
        return objectMapper.valueToTree(courseService.create(new CreateCourseRequest(
                departmentId, name, code, 3, Semester.SEMESTER_1,
                "2026/2027", null, null), "userId:1"));
    }

    private String concurrentCreate(long departmentId, String code, CountDownLatch start)
            throws Exception {
        start.await();
        try {
            createCourse(departmentId, "Concurrent", code);
            return "CREATED";
        } catch (ProblemException exception) {
            return exception.getCode() == ErrorCode.COURSE_CODE_ALREADY_EXISTS
                    ? "DUPLICATE" : exception.getCode().name();
        }
    }

    private String createBody(long departmentId, String name, String code, String status)
            throws Exception {
        return objectMapper.writeValueAsString(new CreateCoursePayload(
                departmentId, name, code, 3, "SEMESTER_1", "2026/2027", null, status));
    }

    private UpdateCourseRequest updateRequest(long departmentId, String name, String code,
                                              long version) {
        return new UpdateCourseRequest(departmentId, name, code, 3, Semester.SEMESTER_1,
                "2026/2027", null, CourseStatus.ACTIVE, version);
    }

    private record CreateCoursePayload(
            long departmentId,
            String courseName,
            String courseCode,
            int creditUnits,
            String semester,
            String academicYear,
            String description,
            String status
    ) {
    }
}
