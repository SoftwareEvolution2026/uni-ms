package com.uni.ms.academiccatalog.course;

import com.uni.ms.academiccatalog.course.api.CreateCourseRequest;
import com.uni.ms.academiccatalog.course.api.UpdateCourseRequest;
import com.uni.ms.academiccatalog.course.application.CourseApplicationService;
import com.uni.ms.academiccatalog.course.application.CourseDepartmentQuery;
import com.uni.ms.academiccatalog.course.domain.Course;
import com.uni.ms.academiccatalog.course.domain.CourseStatus;
import com.uni.ms.academiccatalog.course.domain.Semester;
import com.uni.ms.academiccatalog.course.infrastructure.CourseRepository;
import com.uni.ms.common.audit.AuditService;
import com.uni.ms.common.exception.ErrorCode;
import com.uni.ms.common.exception.ProblemException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseApplicationServiceTest {

    @Mock
    private CourseRepository repository;
    @Mock
    private CourseDepartmentQuery departmentQuery;
    @Mock
    private AuditService auditService;

    @Test
    void createNormalizesValuesAndDefaultsStatus() {
        when(departmentQuery.isAvailable(10L)).thenReturn(true);

        var response = service().create(new CreateCourseRequest(
                10L, " Software Engineering ", " se-101 ", 3,
                Semester.SEMESTER_1, "2026/2027", "   ", null), "userId:1");

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(repository).saveAndFlush(captor.capture());
        Course saved = captor.getValue();
        assertEquals("Software Engineering", saved.getCourseName());
        assertEquals("SE-101", saved.getCourseCode());
        assertNull(saved.getDescription());
        assertEquals(CourseStatus.ACTIVE, response.status());
        verify(auditService).record("userId:1", "COURSE_CREATED",
                "courseId=null, courseCode=SE-101");
    }

    @Test
    void createRejectsUnavailableDepartment() {
        ProblemException exception = assertThrows(ProblemException.class,
                () -> service().create(createRequest(99L, "CS-101"), "userId:1"));

        assertEquals(ErrorCode.COURSE_DEPARTMENT_UNAVAILABLE, exception.getCode());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsDuplicateCodeIncludingDeletedRows() {
        when(departmentQuery.isAvailable(10L)).thenReturn(true);
        when(repository.existsByCourseCodeIgnoreCase("CS-101")).thenReturn(true);

        ProblemException exception = assertThrows(ProblemException.class,
                () -> service().create(createRequest(10L, "cs-101"), "userId:1"));

        assertEquals(ErrorCode.COURSE_CODE_ALREADY_EXISTS, exception.getCode());
    }

    @Test
    void updatePerformsFullReplacementAndNormalization() {
        Course course = course(10L, "OLD");
        when(repository.findById(1L)).thenReturn(Optional.of(course));
        when(departmentQuery.isAvailable(20L)).thenReturn(true);

        service().update(1L, new UpdateCourseRequest(
                20L, " New Course ", " new-1 ", 6, Semester.SUMMER,
                "2027/2028", " Description ", CourseStatus.INACTIVE, 0L), "userId:1");

        assertEquals(20L, course.getDepartmentId());
        assertEquals("New Course", course.getCourseName());
        assertEquals("NEW-1", course.getCourseCode());
        assertEquals("Description", course.getDescription());
        assertEquals(CourseStatus.INACTIVE, course.getStatus());
        verify(auditService).record("userId:1", "COURSE_UPDATED",
                "courseId=null, courseCode=NEW-1");
    }

    @Test
    void updateRejectsStaleVersion() {
        when(repository.findById(1L)).thenReturn(Optional.of(course(10L, "CS-101")));

        ProblemException exception = assertThrows(ProblemException.class,
                () -> service().update(1L, updateRequest(10L, "CS-101", 3L), "userId:1"));

        assertEquals(ErrorCode.COURSE_VERSION_CONFLICT, exception.getCode());
    }

    @Test
    void invalidAcademicYearAndSortAreRejected() {
        when(departmentQuery.isAvailable(10L)).thenReturn(true);
        CreateCourseRequest invalidYear = new CreateCourseRequest(
                10L, "Course", "CODE", 3, Semester.SEMESTER_1,
                "2026/2029", null, CourseStatus.ACTIVE);
        ProblemException year = assertThrows(ProblemException.class,
                () -> service().create(invalidYear, "userId:1"));
        ProblemException sort = assertThrows(ProblemException.class,
                () -> service().search(null, null, null, null, null,
                        0, 10, "passwordHash,desc", false));

        assertEquals(ErrorCode.INVALID_ACADEMIC_YEAR, year.getCode());
        assertEquals(ErrorCode.INVALID_SORT_FIELD, sort.getCode());
    }

    @Test
    void softDeleteAndRestoreEnforceLifecycleAndDepartmentAvailability() {
        Course course = course(10L, "CS-101");
        when(repository.findById(1L)).thenReturn(Optional.of(course));
        CourseApplicationService service = service();

        service.softDelete(1L, "userId:1");
        assertTrue(course.isDeleted());
        assertEquals(ErrorCode.COURSE_ALREADY_DELETED,
                assertThrows(ProblemException.class,
                        () -> service.softDelete(1L, "userId:1")).getCode());

        assertEquals(ErrorCode.COURSE_DEPARTMENT_UNAVAILABLE,
                assertThrows(ProblemException.class,
                        () -> service.restore(1L, "userId:1")).getCode());
        when(departmentQuery.isAvailable(10L)).thenReturn(true);
        service.restore(1L, "userId:1");
        assertNull(course.getDeletedAt());
    }

    @Test
    void permanentDeleteRequiresTrash() {
        Course course = course(10L, "CS-101");
        when(repository.findById(1L)).thenReturn(Optional.of(course));
        CourseApplicationService service = service();

        ProblemException active = assertThrows(ProblemException.class,
                () -> service.permanentlyDelete(1L, "userId:1"));
        assertEquals(ErrorCode.COURSE_NOT_DELETED, active.getCode());

        course.softDelete(Instant.now());
        service.permanentlyDelete(1L, "userId:1");
        verify(repository).delete(course);
        verify(repository).flush();
        verify(auditService).record("userId:1", "COURSE_PERMANENTLY_DELETED",
                "courseId=null, courseCode=CS-101");
    }

    @Test
    void getByIdReturnsNotFoundForDeletedOrMissingCourse() {
        ProblemException exception = assertThrows(ProblemException.class,
                () -> service().getById(99L));
        assertEquals(ErrorCode.COURSE_NOT_FOUND, exception.getCode());
    }

    private CourseApplicationService service() {
        return new CourseApplicationService(repository, departmentQuery, auditService);
    }

    private Course course(Long departmentId, String code) {
        return Course.create(departmentId, "Course", code, 3, Semester.SEMESTER_1,
                "2026/2027", null, CourseStatus.ACTIVE);
    }

    private CreateCourseRequest createRequest(Long departmentId, String code) {
        return new CreateCourseRequest(departmentId, "Course", code, 3,
                Semester.SEMESTER_1, "2026/2027", null, null);
    }

    private UpdateCourseRequest updateRequest(Long departmentId, String code, long version) {
        return new UpdateCourseRequest(departmentId, "Course", code, 3,
                Semester.SEMESTER_1, "2026/2027", null, CourseStatus.ACTIVE, version);
    }
}
