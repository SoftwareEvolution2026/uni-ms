package com.uni.ms.student.service;

import static org.junit.jupiter.api.Assertions.*;
import com.uni.ms.common.audit.AuditService;
import com.uni.ms.common.exception.ApiException;
import com.uni.ms.common.exception.ResourceNotFoundException;
import com.uni.ms.student.dto.CreateStudentRequest;
import com.uni.ms.student.dto.StudentPageResponse;
import com.uni.ms.student.dto.StudentProfileResponse;
import com.uni.ms.student.dto.StudentResponse;
import com.uni.ms.student.dto.UpdateStudentRequest;
import com.uni.ms.student.entity.Student;
import com.uni.ms.student.repository.StudentRepository;
import com.uni.ms.user.User;
import com.uni.ms.user.UserDirectory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserDirectory userDirectory;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private StudentService studentService;

    private User sampleUser;
    private Student sampleStudent;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(10L);
        sampleUser.setEmail("user@uni.ms");

        sampleStudent = new Student();
        sampleStudent.setId(1L);
        sampleStudent.setStudentNumber("STU001");
        sampleStudent.setFullName("Alice");
        sampleStudent.setEmail("alice@uni.ms");
        sampleStudent.setDepartment("CS");
        sampleStudent.setPhone("0123456789");
        sampleStudent.setUserId(10L);
    }

    // -- getById --

    @Test
    void getById_returnsStudentWhenFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        StudentResponse response = studentService.getById(1L);

        assertEquals("STU001", response.studentNumber());
        assertEquals("alice@uni.ms", response.email());
        assertEquals(10L, response.userId());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.getById(999L));
    }

    // -- listAll --

    @Test
    void listAll_returnsAllStudents() {
        Student s2 = new Student();
        s2.setId(2L);
        s2.setStudentNumber("STU002");
        s2.setFullName("Bob");
        s2.setEmail("bob@uni.ms");
        s2.setDepartment("Math");
        s2.setPhone("111");
        s2.setUserId(11L);

        when(studentRepository.findAll()).thenReturn(List.of(sampleStudent, s2));

        List<StudentResponse> result = studentService.listAll();

        assertEquals(2, result.size());
        assertEquals("STU001", result.get(0).studentNumber());
        assertEquals("STU002", result.get(1).studentNumber());
    }

    @Test
    void listAll_returnsEmptyList() {
        when(studentRepository.findAll()).thenReturn(List.of());

        List<StudentResponse> result = studentService.listAll();

        assertEquals(0, result.size());
    }

    // -- searchAndPaginate --

    @Test
    void searchAndPaginate_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "fullName"));
        Student s2 = new Student();
        s2.setId(2L);
        s2.setStudentNumber("STU002");
        s2.setFullName("Bob");
        s2.setEmail("bob@uni.ms");
        s2.setDepartment("Math");
        s2.setPhone("111");
        s2.setUserId(11L);

        Page<Student> page = new PageImpl<>(List.of(sampleStudent, s2), pageable, 2);
        when(studentRepository.search(eq(""), eq(""), any(Pageable.class))).thenReturn(page);

        StudentPageResponse result = studentService.searchAndPaginate("", "", 0, 20, "fullName,asc");

        assertEquals(2, result.content().size());
        assertEquals(0, result.page());
        assertEquals(20, result.size());
        assertEquals(2, result.totalElements());
        assertEquals(1, result.totalPages());
    }

    @Test
    void searchAndPaginate_filtersByQuery() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "fullName"));
        Page<Student> page = new PageImpl<>(List.of(sampleStudent), pageable, 1);
        when(studentRepository.search(eq("Alice"), eq(""), any(Pageable.class))).thenReturn(page);

        StudentPageResponse result = studentService.searchAndPaginate("Alice", "", 0, 20, "fullName,asc");

        assertEquals(1, result.content().size());
        assertEquals("Alice", result.content().get(0).fullName());
        verify(studentRepository).search(eq("Alice"), eq(""), any(Pageable.class));
    }

    @Test
    void searchAndPaginate_filtersByDepartment() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "studentNumber"));
        Page<Student> page = new PageImpl<>(List.of(sampleStudent), pageable, 1);
        when(studentRepository.search(eq(""), eq("CS"), any(Pageable.class))).thenReturn(page);

        StudentPageResponse result = studentService.searchAndPaginate("", "CS", 0, 10, "studentNumber,desc");

        assertEquals(1, result.content().size());
        assertEquals("CS", result.content().get(0).department());
    }

    @Test
    void searchAndPaginate_returnsEmptyPageWhenNoResults() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "fullName"));
        Page<Student> page = new PageImpl<>(List.of(), pageable, 0);
        when(studentRepository.search(eq("nonexistent"), eq(""), any(Pageable.class))).thenReturn(page);

        StudentPageResponse result = studentService.searchAndPaginate("nonexistent", "", 0, 20, "fullName,asc");

        assertEquals(0, result.content().size());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    void searchAndPaginate_handlesDescSort() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "email"));
        Page<Student> page = new PageImpl<>(List.of(sampleStudent), pageable, 1);
        when(studentRepository.search(eq(""), eq(""), any(Pageable.class))).thenReturn(page);

        StudentPageResponse result = studentService.searchAndPaginate("", "", 0, 20, "email,desc");

        assertNotNull(result);
        assertEquals(1, result.content().size());
    }

    @Test
    void searchAndPaginate_throwsOnInvalidSortField() {
        ApiException ex = assertThrows(ApiException.class,
                () -> studentService.searchAndPaginate("", "", 0, 20, "nonExistentField,asc"));
        assertEquals("Invalid sort field: nonExistentField", ex.getMessage());
    }

    @Test
    void searchAndPaginate_throwsOnNegativePage() {
        ApiException ex = assertThrows(ApiException.class,
                () -> studentService.searchAndPaginate("", "", -1, 20, "fullName,asc"));
        assertEquals("Page must not be negative", ex.getMessage());
    }

    @Test
    void searchAndPaginate_clampsOversizedPageSize() {
        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "fullName"));
        Page<Student> page = new PageImpl<>(List.of(sampleStudent), pageable, 1);
        when(studentRepository.search(eq(""), eq(""), any(Pageable.class))).thenReturn(page);

        StudentPageResponse result = studentService.searchAndPaginate("", "", 0, 1000000, "fullName,asc");

        assertNotNull(result);
        assertEquals(100, result.size());
    }

    // -- getProfile --

    @Test
    void getProfile_returnsProfileWhenFound() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        StudentProfileResponse response = studentService.getProfile(1L);

        assertEquals("STU001", response.studentNumber());
        assertEquals("Alice", response.fullName());
        assertEquals("alice@uni.ms", response.email());
        assertEquals("CS", response.department());
        assertEquals("0123456789", response.phone());
        assertEquals(10L, response.userId());
    }

    @Test
    void getProfile_throwsWhenNotFound() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> studentService.getProfile(999L));
    }

    // -- create --

    @Test
    void create_savesStudentWhenValid() {
        when(studentRepository.existsByEmail("new@uni.ms")).thenReturn(false);
        when(studentRepository.existsByStudentNumber("NEW001")).thenReturn(false);
        when(userDirectory.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> {
            Student s = inv.getArgument(0);
            s.setId(5L);
            return s;
        });

        CreateStudentRequest request = new CreateStudentRequest(
                "NEW001", "New Student", "new@uni.ms", "CS", "0123456789", 10L);

        StudentResponse response = studentService.create(request, "admin@uni.ms");

        assertNotNull(response);
        assertEquals("NEW001", response.studentNumber());
        assertEquals("new@uni.ms", response.email());
        verify(auditService).record("admin@uni.ms", "STUDENT_CREATED", "Created student new@uni.ms");
    }

    @Test
    void create_throwsOnDuplicateEmail() {
        when(studentRepository.existsByEmail("dup@uni.ms")).thenReturn(true);

        CreateStudentRequest request = new CreateStudentRequest(
                "STU999", "Dup", "dup@uni.ms", "CS", "0123456789", 10L);

        ApiException ex = assertThrows(ApiException.class,
                () -> studentService.create(request, "admin@uni.ms"));
        assertEquals("Email already registered for a student", ex.getMessage());
    }

    @Test
    void create_throwsOnDuplicateStudentNumber() {
        when(studentRepository.existsByEmail("ok@uni.ms")).thenReturn(false);
        when(studentRepository.existsByStudentNumber("STU001")).thenReturn(true);

        CreateStudentRequest request = new CreateStudentRequest(
                "STU001", "Dup Num", "ok@uni.ms", "CS", "0123456789", 10L);

        ApiException ex = assertThrows(ApiException.class,
                () -> studentService.create(request, "admin@uni.ms"));
        assertEquals("Student number already exists", ex.getMessage());
    }

    @Test
    void create_throwsWhenUserNotFound() {
        when(studentRepository.existsByEmail("ok@uni.ms")).thenReturn(false);
        when(studentRepository.existsByStudentNumber("NEW001")).thenReturn(false);
        when(userDirectory.findById(999L)).thenReturn(Optional.empty());

        CreateStudentRequest request = new CreateStudentRequest(
                "NEW001", "No User", "ok@uni.ms", "CS", "0123456789", 999L);

        assertThrows(ResourceNotFoundException.class,
                () -> studentService.create(request, "admin@uni.ms"));
        verify(studentRepository, never()).save(any());
    }

    // -- update --

    @Test
    void update_modifiesFieldsWhenValid() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateStudentRequest request = new UpdateStudentRequest(
                "STU001", "Alice Updated", "alice@uni.ms", "Math", "9999");

        StudentResponse response = studentService.update(1L, request, "admin@uni.ms");

        assertEquals("Alice Updated", response.fullName());
        assertEquals("Math", response.department());
        assertEquals("9999", response.phone());
        verify(auditService).record("admin@uni.ms", "STUDENT_UPDATED", "Updated student alice@uni.ms");
    }

    @Test
    void update_throwsWhenNotFound() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateStudentRequest request = new UpdateStudentRequest(
                "X", "X", "x@x.ms", "X", "0");

        assertThrows(ResourceNotFoundException.class,
                () -> studentService.update(999L, request, "admin@uni.ms"));
    }

    @Test
    void update_throwsOnDuplicateEmailWhenEmailChanged() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.existsByEmail("taken@uni.ms")).thenReturn(true);

        UpdateStudentRequest request = new UpdateStudentRequest(
                "STU001", "Alice", "taken@uni.ms", "CS", "0123456789");

        ApiException ex = assertThrows(ApiException.class,
                () -> studentService.update(1L, request, "admin@uni.ms"));
        assertEquals("Email already registered for a student", ex.getMessage());
    }

    @Test
    void update_allowsSameEmailWithoutConflict() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateStudentRequest request = new UpdateStudentRequest(
                "STU001", "Alice", "alice@uni.ms", "CS", "0123456789");

        StudentResponse response = studentService.update(1L, request, "admin@uni.ms");

        assertEquals("alice@uni.ms", response.email());
    }

    @Test
    void update_throwsOnDuplicateStudentNumberWhenChanged() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.existsByStudentNumber("TAKEN")).thenReturn(true);

        UpdateStudentRequest request = new UpdateStudentRequest(
                "TAKEN", "Alice", "alice@uni.ms", "CS", "0123456789");

        ApiException ex = assertThrows(ApiException.class,
                () -> studentService.update(1L, request, "admin@uni.ms"));
        assertEquals("Student number already exists", ex.getMessage());
    }

    @Test
    void update_allowsSameStudentNumberWithoutConflict() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));
        when(studentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateStudentRequest request = new UpdateStudentRequest(
                "STU001", "Alice", "alice@uni.ms", "CS", "0123456789");

        StudentResponse response = studentService.update(1L, request, "admin@uni.ms");

        assertEquals("STU001", response.studentNumber());
    }

    // -- delete --

    @Test
    void delete_removesStudent() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(sampleStudent));

        studentService.delete(1L, "admin@uni.ms");

        verify(studentRepository).delete(sampleStudent);
        verify(auditService).record("admin@uni.ms", "STUDENT_DELETED", "Deleted student alice@uni.ms");
    }

    @Test
    void delete_throwsWhenNotFound() {
        when(studentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> studentService.delete(999L, "admin@uni.ms"));
        verify(studentRepository, never()).delete(any());
    }
}
