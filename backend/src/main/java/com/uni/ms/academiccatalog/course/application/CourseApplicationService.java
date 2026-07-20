package com.uni.ms.academiccatalog.course.application;

import com.uni.ms.academiccatalog.course.api.CourseResponse;
import com.uni.ms.academiccatalog.course.api.CreateCourseRequest;
import com.uni.ms.academiccatalog.course.api.UpdateCourseRequest;
import com.uni.ms.academiccatalog.course.domain.Course;
import com.uni.ms.academiccatalog.course.domain.CourseStatus;
import com.uni.ms.academiccatalog.course.domain.Semester;
import com.uni.ms.academiccatalog.course.infrastructure.CourseRepository;
import com.uni.ms.academiccatalog.course.infrastructure.CourseSpecifications;
import com.uni.ms.common.api.PageResponse;
import com.uni.ms.common.audit.AuditService;
import com.uni.ms.common.exception.ErrorCode;
import com.uni.ms.common.exception.ProblemException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseApplicationService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "courseName", "courseCode", "creditUnits", "semester", "academicYear",
            "status", "createdAt", "updatedAt");

    private final CourseRepository courseRepository;
    private final CourseDepartmentQuery departmentQuery;
    private final AuditService auditService;

    @Transactional
    public CourseResponse create(CreateCourseRequest request, String actor) {
        ensureDepartmentAvailable(request.departmentId());
        String code = normalizeCode(request.courseCode());
        ensureCodeAvailable(code, null);
        String academicYear = normalizeAcademicYear(request.academicYear());
        Course course = Course.create(request.departmentId(), normalizeRequired(request.courseName()),
                code, request.creditUnits(), request.semester(), academicYear,
                normalizeDescription(request.description()), request.status());
        try {
            courseRepository.saveAndFlush(course);
        } catch (DataIntegrityViolationException ex) {
            throw translateIntegrityViolation(ex);
        }
        auditService.record(actor, "COURSE_CREATED", auditDetail(course));
        return CourseResponse.from(course);
    }

    @Transactional(readOnly = true)
    public CourseResponse getById(Long id) {
        return CourseResponse.from(courseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(this::notFound));
    }

    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> search(String search, Long departmentId,
                                               String semester, String academicYear,
                                               String status, int page, int size,
                                               String sort, boolean deleted) {
        CourseSearchCriteria criteria = new CourseSearchCriteria(
                normalizeOptional(search), departmentId, parseSemester(semester),
                normalizeOptional(academicYear), parseStatus(status), deleted);
        Page<Course> result = courseRepository.findAll(
                CourseSpecifications.matching(criteria),
                PageRequest.of(page, size, parseSort(sort)));
        List<CourseResponse> content = result.getContent().stream()
                .map(CourseResponse::from).toList();
        return PageResponse.from(result, content);
    }

    @Transactional
    public CourseResponse update(Long id, UpdateCourseRequest request, String actor) {
        Course course = courseRepository.findById(id).orElseThrow(this::notFound);
        if (course.isDeleted()) {
            throw alreadyDeleted();
        }
        if (course.getVersion() != request.version()) {
            throw versionConflict();
        }
        ensureDepartmentAvailable(request.departmentId());
        String code = normalizeCode(request.courseCode());
        ensureCodeAvailable(code, id);
        course.update(request.departmentId(), normalizeRequired(request.courseName()), code,
                request.creditUnits(), request.semester(),
                normalizeAcademicYear(request.academicYear()),
                normalizeDescription(request.description()), request.status());
        try {
            courseRepository.saveAndFlush(course);
        } catch (OptimisticLockingFailureException ex) {
            throw versionConflict();
        } catch (DataIntegrityViolationException ex) {
            throw translateIntegrityViolation(ex);
        }
        auditService.record(actor, "COURSE_UPDATED", auditDetail(course));
        return CourseResponse.from(course);
    }

    @Transactional
    public void softDelete(Long id, String actor) {
        Course course = courseRepository.findById(id).orElseThrow(this::notFound);
        if (course.isDeleted()) {
            throw alreadyDeleted();
        }
        course.softDelete(Instant.now());
        courseRepository.saveAndFlush(course);
        auditService.record(actor, "COURSE_DELETED", auditDetail(course));
    }

    @Transactional
    public CourseResponse restore(Long id, String actor) {
        Course course = courseRepository.findById(id).orElseThrow(this::notFound);
        if (!course.isDeleted()) {
            throw new ProblemException(HttpStatus.CONFLICT, ErrorCode.COURSE_NOT_DELETED,
                    "Course is not deleted");
        }
        ensureDepartmentAvailable(course.getDepartmentId());
        ensureCodeAvailable(course.getCourseCode(), id);
        course.restore();
        try {
            courseRepository.saveAndFlush(course);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateCode();
        }
        auditService.record(actor, "COURSE_RESTORED", auditDetail(course));
        return CourseResponse.from(course);
    }

    @Transactional
    public void permanentlyDelete(Long id, String actor) {
        Course course = courseRepository.findById(id).orElseThrow(this::notFound);
        if (!course.isDeleted()) {
            throw new ProblemException(HttpStatus.CONFLICT, ErrorCode.COURSE_NOT_DELETED,
                    "Course must be soft-deleted before permanent deletion");
        }
        courseRepository.delete(course);
        courseRepository.flush();
        auditService.record(actor, "COURSE_PERMANENTLY_DELETED", auditDetail(course));
    }

    private void ensureDepartmentAvailable(Long departmentId) {
        if (!departmentQuery.isAvailable(departmentId)) {
            throw departmentUnavailable();
        }
    }

    private void ensureCodeAvailable(String code, Long excludedId) {
        boolean exists = excludedId == null
                ? courseRepository.existsByCourseCodeIgnoreCase(code)
                : courseRepository.existsByCourseCodeIgnoreCaseAndIdNot(code, excludedId);
        if (exists) {
            throw duplicateCode();
        }
    }

    private String normalizeAcademicYear(String value) {
        String normalized = value.trim();
        String[] years = normalized.split("/");
        if (years.length != 2) {
            throw invalidAcademicYear();
        }
        try {
            int first = Integer.parseInt(years[0]);
            int second = Integer.parseInt(years[1]);
            if (second != first + 1) {
                throw invalidAcademicYear();
            }
        } catch (NumberFormatException ex) {
            throw invalidAcademicYear();
        }
        return normalized;
    }

    private Semester parseSemester(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Semester.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ProblemException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_SEMESTER,
                    "Semester must be SEMESTER_1, SEMESTER_2 or SUMMER");
        }
    }

    private CourseStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CourseStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ProblemException(HttpStatus.BAD_REQUEST,
                    ErrorCode.INVALID_COURSE_STATUS,
                    "Course status must be ACTIVE or INACTIVE");
        }
    }

    private Sort parseSort(String value) {
        String normalized = value == null || value.isBlank() ? "courseName,asc" : value.trim();
        String[] parts = normalized.split(",", -1);
        if (parts.length > 2 || !SORTABLE_FIELDS.contains(parts[0])) {
            throw invalidSort();
        }
        try {
            Sort.Direction direction = parts.length == 1 || parts[1].isBlank()
                    ? Sort.Direction.ASC : Sort.Direction.fromString(parts[1]);
            return Sort.by(direction, parts[0]);
        } catch (IllegalArgumentException ex) {
            throw invalidSort();
        }
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }

    private String normalizeDescription(String value) {
        return normalizeOptional(value);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String auditDetail(Course course) {
        return "courseId=" + course.getId() + ", courseCode=" + course.getCourseCode();
    }

    private ProblemException notFound() {
        return new ProblemException(HttpStatus.NOT_FOUND, ErrorCode.COURSE_NOT_FOUND,
                "Course was not found");
    }

    private ProblemException alreadyDeleted() {
        return new ProblemException(HttpStatus.CONFLICT, ErrorCode.COURSE_ALREADY_DELETED,
                "Course is already deleted");
    }

    private ProblemException duplicateCode() {
        return new ProblemException(HttpStatus.CONFLICT,
                ErrorCode.COURSE_CODE_ALREADY_EXISTS,
                "A course with this code already exists");
    }

    private ProblemException departmentUnavailable() {
        return new ProblemException(HttpStatus.CONFLICT,
                ErrorCode.COURSE_DEPARTMENT_UNAVAILABLE,
                "Course Department does not exist, is inactive or is deleted");
    }

    private ProblemException translateIntegrityViolation(DataIntegrityViolationException ex) {
        String databaseMessage = ex.getMostSpecificCause().getMessage();
        if (databaseMessage != null && databaseMessage.contains("fk_courses_department")) {
            return departmentUnavailable();
        }
        return duplicateCode();
    }

    private ProblemException versionConflict() {
        return new ProblemException(HttpStatus.CONFLICT, ErrorCode.COURSE_VERSION_CONFLICT,
                "Course was modified by another request");
    }

    private ProblemException invalidSort() {
        return new ProblemException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_SORT_FIELD,
                "Unsupported Course sort field or direction");
    }

    private ProblemException invalidAcademicYear() {
        return new ProblemException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ACADEMIC_YEAR,
                "Academic year must be consecutive years in YYYY/YYYY format");
    }
}
