package com.uni.ms.academiccatalog.department;

import com.uni.ms.academiccatalog.department.api.CreateDepartmentRequest;
import com.uni.ms.academiccatalog.department.api.UpdateDepartmentRequest;
import com.uni.ms.academiccatalog.department.application.DepartmentApplicationService;
import com.uni.ms.academiccatalog.department.application.DepartmentCourseReferenceQuery;
import com.uni.ms.academiccatalog.department.domain.Department;
import com.uni.ms.academiccatalog.department.domain.DepartmentStatus;
import com.uni.ms.academiccatalog.department.infrastructure.DepartmentRepository;
import com.uni.ms.common.audit.AuditService;
import com.uni.ms.common.exception.ErrorCode;
import com.uni.ms.common.exception.ProblemException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
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
class DepartmentApplicationServiceTest {

    @Mock
    private DepartmentRepository repository;
    @Mock
    private DepartmentCourseReferenceQuery courseReferenceQuery;
    @Mock
    private AuditService auditService;

    @Test
    void createNormalizesValuesAndDefaultsStatusToActive() {
        DepartmentApplicationService service = service();

        var response = service.create(new CreateDepartmentRequest(
                " Computer Science ", " cs ", " Science ", "   ", null), "userId:1");

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(repository).saveAndFlush(captor.capture());
        Department saved = captor.getValue();
        assertEquals("Computer Science", saved.getDepartmentName());
        assertEquals("CS", saved.getDepartmentCode());
        assertEquals("Science", saved.getFaculty());
        assertNull(saved.getDescription());
        assertEquals(DepartmentStatus.ACTIVE, saved.getStatus());
        assertEquals(DepartmentStatus.ACTIVE, response.status());
    }

    @Test
    void createRejectsDuplicateCodeIncludingDeletedRows() {
        when(repository.existsByDepartmentCodeIgnoreCase("CS")).thenReturn(true);

        ProblemException exception = assertThrows(ProblemException.class, () -> service().create(
                new CreateDepartmentRequest("Computer Science", "CS", "Science", null,
                        DepartmentStatus.ACTIVE), "userId:1"));

        assertEquals(ErrorCode.DEPARTMENT_CODE_ALREADY_EXISTS, exception.getCode());
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateNormalizesFullReplacement() {
        Department department = Department.create("Old", "OLD", "Old Faculty", null,
                DepartmentStatus.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(department));
        when(courseReferenceQuery.countByDepartmentIds(any())).thenReturn(Map.of());

        service().update(1L, new UpdateDepartmentRequest(
                " New Name ", " new ", " New Faculty ", " Description ",
                DepartmentStatus.INACTIVE, 0L), "userId:1");

        assertEquals("New Name", department.getDepartmentName());
        assertEquals("NEW", department.getDepartmentCode());
        assertEquals("New Faculty", department.getFaculty());
        assertEquals("Description", department.getDescription());
        assertEquals(DepartmentStatus.INACTIVE, department.getStatus());
    }

    @Test
    void staleVersionIsRejected() {
        Department department = Department.create("Name", "CODE", "Faculty", null,
                DepartmentStatus.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(department));

        ProblemException exception = assertThrows(ProblemException.class, () -> service().update(
                1L, new UpdateDepartmentRequest("Name", "CODE", "Faculty", null,
                        DepartmentStatus.ACTIVE, 3L), "userId:1"));

        assertEquals(ErrorCode.DEPARTMENT_VERSION_CONFLICT, exception.getCode());
    }

    @Test
    void softDeleteSetsTimestampAndRepeatedDeleteConflicts() {
        Department department = Department.create("Name", "CODE", "Faculty", null,
                DepartmentStatus.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(department));
        DepartmentApplicationService service = service();

        service.softDelete(1L, "userId:1");
        assertTrue(department.isDeleted());

        ProblemException exception = assertThrows(ProblemException.class,
                () -> service.softDelete(1L, "userId:1"));
        assertEquals(ErrorCode.DEPARTMENT_ALREADY_DELETED, exception.getCode());
    }

    @Test
    void restorePreservesStatusAndRejectsActiveDepartment() {
        Department active = Department.create("Active", "ACT", "Faculty", null,
                DepartmentStatus.INACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        ProblemException exception = assertThrows(ProblemException.class,
                () -> service().restore(1L, "userId:1"));
        assertEquals(ErrorCode.DEPARTMENT_NOT_DELETED, exception.getCode());

        active.softDelete(java.time.Instant.now());
        when(courseReferenceQuery.countByDepartmentIds(any())).thenReturn(Map.of());
        service().restore(1L, "userId:1");
        assertEquals(DepartmentStatus.INACTIVE, active.getStatus());
        assertNull(active.getDeletedAt());
    }

    @Test
    void permanentDeleteRequiresTrashAndNoCourseReferences() {
        Department active = Department.create("Name", "CODE", "Faculty", null,
                DepartmentStatus.ACTIVE);
        when(repository.findById(1L)).thenReturn(Optional.of(active));
        ProblemException notDeleted = assertThrows(ProblemException.class,
                () -> service().permanentlyDelete(1L, "userId:1"));
        assertEquals(ErrorCode.DEPARTMENT_NOT_DELETED, notDeleted.getCode());

        active.softDelete(java.time.Instant.now());
        when(courseReferenceQuery.hasAnyCourse(1L)).thenReturn(true);
        ProblemException referenced = assertThrows(ProblemException.class,
                () -> service().permanentlyDelete(1L, "userId:1"));
        assertEquals(ErrorCode.DEPARTMENT_HAS_COURSES, referenced.getCode());
        verify(repository, never()).delete(active);
    }

    @Test
    void permanentDeleteRemovesUnreferencedTrashEntry() {
        Department department = Department.create("Name", "CODE", "Faculty", null,
                DepartmentStatus.ACTIVE);
        department.softDelete(java.time.Instant.now());
        when(repository.findById(1L)).thenReturn(Optional.of(department));

        service().permanentlyDelete(1L, "userId:1");

        verify(repository).delete(department);
        verify(repository).flush();
        verify(auditService).record("userId:1", "DEPARTMENT_PERMANENTLY_DELETED",
                "departmentId=null, departmentCode=CODE");
    }

    @Test
    void invalidSortFieldIsRejectedBeforeRepositoryQuery() {
        ProblemException exception = assertThrows(ProblemException.class,
                () -> service().search(null, null, null, 0, 10,
                        "passwordHash,desc", false));

        assertEquals(ErrorCode.INVALID_SORT_FIELD, exception.getCode());
    }

    private DepartmentApplicationService service() {
        return new DepartmentApplicationService(repository, courseReferenceQuery, auditService);
    }
}
