package com.uni.ms.academiccatalog.department.application;

import com.uni.ms.academiccatalog.department.api.CreateDepartmentRequest;
import com.uni.ms.academiccatalog.department.api.DepartmentResponse;
import com.uni.ms.academiccatalog.department.api.UpdateDepartmentRequest;
import com.uni.ms.academiccatalog.department.domain.Department;
import com.uni.ms.academiccatalog.department.domain.DepartmentStatus;
import com.uni.ms.academiccatalog.department.infrastructure.DepartmentRepository;
import com.uni.ms.academiccatalog.department.infrastructure.DepartmentSpecifications;
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
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DepartmentApplicationService {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "departmentName", "departmentCode", "faculty", "status",
            "createdAt", "updatedAt");

    private final DepartmentRepository departmentRepository;
    private final DepartmentCourseReferenceQuery courseReferenceQuery;
    private final AuditService auditService;

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request, String actor) {
        String code = normalizeCode(request.departmentCode());
        ensureCodeAvailable(code, null);
        Department department = Department.create(
                normalizeRequired(request.departmentName()),
                code,
                normalizeRequired(request.faculty()),
                normalizeDescription(request.description()),
                request.status());
        try {
            departmentRepository.saveAndFlush(department);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateCode();
        }
        auditService.record(actor, "DEPARTMENT_CREATED", auditDetail(department));
        return DepartmentResponse.from(department, 0);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse getById(Long id) {
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(this::notFound);
        return DepartmentResponse.from(department,
                courseReferenceQuery.countByDepartmentIds(List.of(id)).getOrDefault(id, 0L));
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> search(String search, String status, String faculty,
                                                   int page, int size, String sort,
                                                   boolean deleted) {
        DepartmentSearchCriteria criteria = new DepartmentSearchCriteria(
                normalizeOptional(search), parseStatus(status), normalizeOptional(faculty),
                deleted);
        PageRequest pageRequest = PageRequest.of(page, size, parseSort(sort));
        Page<Department> result = departmentRepository.findAll(
                DepartmentSpecifications.matching(criteria), pageRequest);
        List<Long> ids = result.getContent().stream().map(Department::getId).toList();
        Map<Long, Long> counts = courseReferenceQuery.countByDepartmentIds(ids);
        List<DepartmentResponse> content = result.getContent().stream()
                .map(department -> DepartmentResponse.from(department,
                        counts.getOrDefault(department.getId(), 0L)))
                .toList();
        return PageResponse.from(result, content);
    }

    @Transactional
    public DepartmentResponse update(Long id, UpdateDepartmentRequest request, String actor) {
        Department department = departmentRepository.findById(id).orElseThrow(this::notFound);
        if (department.isDeleted()) {
            throw alreadyDeleted();
        }
        if (department.getVersion() != request.version()) {
            throw versionConflict();
        }
        String code = normalizeCode(request.departmentCode());
        ensureCodeAvailable(code, id);
        department.update(
                normalizeRequired(request.departmentName()),
                code,
                normalizeRequired(request.faculty()),
                normalizeDescription(request.description()),
                request.status());
        try {
            departmentRepository.saveAndFlush(department);
        } catch (OptimisticLockingFailureException ex) {
            throw versionConflict();
        } catch (DataIntegrityViolationException ex) {
            throw duplicateCode();
        }
        auditService.record(actor, "DEPARTMENT_UPDATED", auditDetail(department));
        long courseCount = courseReferenceQuery.countByDepartmentIds(List.of(id))
                .getOrDefault(id, 0L);
        return DepartmentResponse.from(department, courseCount);
    }

    @Transactional
    public void softDelete(Long id, String actor) {
        Department department = departmentRepository.findById(id).orElseThrow(this::notFound);
        if (department.isDeleted()) {
            throw alreadyDeleted();
        }
        department.softDelete(Instant.now());
        departmentRepository.saveAndFlush(department);
        auditService.record(actor, "DEPARTMENT_DELETED", auditDetail(department));
    }

    @Transactional
    public DepartmentResponse restore(Long id, String actor) {
        Department department = departmentRepository.findById(id).orElseThrow(this::notFound);
        if (!department.isDeleted()) {
            throw new ProblemException(HttpStatus.CONFLICT,
                    ErrorCode.DEPARTMENT_NOT_DELETED, "Department is not deleted");
        }
        ensureCodeAvailable(department.getDepartmentCode(), id);
        department.restore();
        try {
            departmentRepository.saveAndFlush(department);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateCode();
        }
        auditService.record(actor, "DEPARTMENT_RESTORED", auditDetail(department));
        long courseCount = courseReferenceQuery.countByDepartmentIds(List.of(id))
                .getOrDefault(id, 0L);
        return DepartmentResponse.from(department, courseCount);
    }

    @Transactional
    public void permanentlyDelete(Long id, String actor) {
        Department department = departmentRepository.findById(id).orElseThrow(this::notFound);
        if (!department.isDeleted()) {
            throw new ProblemException(HttpStatus.CONFLICT,
                    ErrorCode.DEPARTMENT_NOT_DELETED,
                    "Department must be soft-deleted before permanent deletion");
        }
        if (courseReferenceQuery.hasAnyCourse(id)) {
            throw new ProblemException(HttpStatus.CONFLICT,
                    ErrorCode.DEPARTMENT_HAS_COURSES,
                    "Department cannot be permanently deleted while courses reference it");
        }
        try {
            departmentRepository.delete(department);
            departmentRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ProblemException(HttpStatus.CONFLICT,
                    ErrorCode.DEPARTMENT_HAS_COURSES,
                    "Department cannot be permanently deleted while courses reference it");
        }
        auditService.record(actor, "DEPARTMENT_PERMANENTLY_DELETED",
                auditDetail(department));
    }

    private void ensureCodeAvailable(String code, Long excludedId) {
        boolean exists = excludedId == null
                ? departmentRepository.existsByDepartmentCodeIgnoreCase(code)
                : departmentRepository.existsByDepartmentCodeIgnoreCaseAndIdNot(code, excludedId);
        if (exists) {
            throw duplicateCode();
        }
    }

    private Sort parseSort(String value) {
        String normalized = value == null || value.isBlank() ? "departmentName,asc" : value.trim();
        String[] parts = normalized.split(",", -1);
        if (parts.length > 2 || !SORTABLE_FIELDS.contains(parts[0])) {
            throw invalidSort();
        }
        Sort.Direction direction;
        try {
            direction = parts.length == 1 || parts[1].isBlank()
                    ? Sort.Direction.ASC
                    : Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException ex) {
            throw invalidSort();
        }
        return Sort.by(direction, parts[0]);
    }

    private DepartmentStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return DepartmentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ProblemException(HttpStatus.BAD_REQUEST,
                    ErrorCode.INVALID_DEPARTMENT_STATUS,
                    "Department status must be ACTIVE or INACTIVE");
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
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String auditDetail(Department department) {
        return "departmentId=" + department.getId() + ", departmentCode="
                + department.getDepartmentCode();
    }

    private ProblemException notFound() {
        return new ProblemException(HttpStatus.NOT_FOUND, ErrorCode.DEPARTMENT_NOT_FOUND,
                "Department was not found");
    }

    private ProblemException alreadyDeleted() {
        return new ProblemException(HttpStatus.CONFLICT,
                ErrorCode.DEPARTMENT_ALREADY_DELETED, "Department is already deleted");
    }

    private ProblemException duplicateCode() {
        return new ProblemException(HttpStatus.CONFLICT,
                ErrorCode.DEPARTMENT_CODE_ALREADY_EXISTS,
                "A department with this code already exists");
    }

    private ProblemException versionConflict() {
        return new ProblemException(HttpStatus.CONFLICT,
                ErrorCode.DEPARTMENT_VERSION_CONFLICT,
                "Department was modified by another request");
    }

    private ProblemException invalidSort() {
        return new ProblemException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_SORT_FIELD,
                "Unsupported Department sort field or direction");
    }
}
