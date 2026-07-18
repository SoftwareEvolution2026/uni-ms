package com.uni.ms.student.controller;

import com.uni.ms.common.exception.ApiException;
import com.uni.ms.common.exception.ResourceNotFoundException;
import com.uni.ms.student.controller.StudentController;
import com.uni.ms.student.dto.CreateStudentRequest;
import com.uni.ms.student.dto.StudentResponse;
import com.uni.ms.student.dto.UpdateStudentRequest;
import com.uni.ms.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    private StudentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new StudentResponse(1L, "STU001", "Alice",
                "alice@uni.ms", "CS", "0123456789", 10L);
    }

    // -- getById --

    @Test
    void getById_delegatesToService() {
        when(studentService.getById(1L)).thenReturn(sampleResponse);

        StudentResponse result = studentController.getById(1L);

        assertEquals("STU001", result.studentNumber());
        assertEquals("alice@uni.ms", result.email());
        verify(studentService).getById(1L);
    }

    @Test
    void getById_propagatesNotFound() {
        when(studentService.getById(999L))
                .thenThrow(new ResourceNotFoundException("Student not found: 999"));

        assertThrows(ResourceNotFoundException.class,
                () -> studentController.getById(999L));
    }

    // -- list --

    @Test
    void list_delegatesToService() {
        StudentResponse s2 = new StudentResponse(2L, "STU002", "Bob",
                "bob@uni.ms", "Math", "111", 11L);
        when(studentService.listAll()).thenReturn(List.of(sampleResponse, s2));

        List<StudentResponse> result = studentController.list();

        assertEquals(2, result.size());
        assertEquals("STU002", result.get(1).studentNumber());
    }

    // -- create --

    @Test
    void create_returns201WithStudent() {
        CreateStudentRequest request = new CreateStudentRequest(
                "STU001", "Alice", "alice@uni.ms", "CS", "0123456789", 10L);
        when(studentService.create(any(CreateStudentRequest.class), eq("admin@uni.ms")))
                .thenReturn(sampleResponse);

        ResponseEntity<StudentResponse> result =
                studentController.create(request, fakePrincipal("admin@uni.ms"));

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("STU001", result.getBody().studentNumber());
    }

    @Test
    void create_propagatesConflict() {
        CreateStudentRequest request = new CreateStudentRequest(
                "STU001", "Dup", "dup@uni.ms", "CS", "0123456789", 10L);
        when(studentService.create(any(CreateStudentRequest.class), eq("admin@uni.ms")))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "Email already registered"));

        assertThrows(ApiException.class,
                () -> studentController.create(request, fakePrincipal("admin@uni.ms")));
    }

    // -- update --

    @Test
    void update_delegatesToService() {
        UpdateStudentRequest request = new UpdateStudentRequest(
                "STU001", "Alice Updated", "alice@uni.ms", "CS", "0123456789");
        when(studentService.update(eq(1L), any(UpdateStudentRequest.class), eq("admin@uni.ms")))
                .thenReturn(sampleResponse);

        StudentResponse result = studentController.update(1L, request,
                fakePrincipal("admin@uni.ms"));

        assertEquals("STU001", result.studentNumber());
    }

    @Test
    void update_propagatesNotFound() {
        UpdateStudentRequest request = new UpdateStudentRequest(
                "X", "X", "x@x.ms", "X", "0");
        when(studentService.update(eq(999L), any(UpdateStudentRequest.class), eq("admin@uni.ms")))
                .thenThrow(new ResourceNotFoundException("Student not found: 999"));

        assertThrows(ResourceNotFoundException.class,
                () -> studentController.update(999L, request, fakePrincipal("admin@uni.ms")));
    }

    // -- delete --

    @Test
    void delete_returns204() {
        ResponseEntity<Void> result = studentController.delete(1L,
                fakePrincipal("admin@uni.ms"));

        assertEquals(HttpStatus.NO_CONTENT, result.getStatusCode());
        verify(studentService).delete(1L, "admin@uni.ms");
    }

    @Test
    void delete_propagatesNotFound() {
        doThrow(new ResourceNotFoundException("Student not found: 999"))
                .when(studentService).delete(eq(999L), eq("admin@uni.ms"));

        assertThrows(ResourceNotFoundException.class,
                () -> studentController.delete(999L, fakePrincipal("admin@uni.ms")));
    }

    // -- helper --

    private static org.springframework.security.core.userdetails.UserDetails fakePrincipal(
            String email) {
        return new org.springframework.security.core.userdetails.User(
                email, "irrelevant", List.of());
    }
}
