package com.uni.ms.result.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.uni.ms.common.audit.AuditService;
import com.uni.ms.common.exception.ApiException;
import com.uni.ms.common.exception.ResourceNotFoundException;
import com.uni.ms.result.dto.CreateResultRequest;
import com.uni.ms.result.dto.UpdateResultRequest;
import com.uni.ms.result.entity.Result;
import com.uni.ms.result.repository.ResultRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private ResultRepository resultRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ResultService resultService;

    private Result sampleResult;

    @BeforeEach
    void setUp() {
        sampleResult = new Result();
        sampleResult.setId(1L);
        sampleResult.setStudentId(10L);
        sampleResult.setCourseCode("CS101");
        sampleResult.setTerm("Spring 2026");
        sampleResult.setGrade("A");
        sampleResult.setScore(92.5);
        sampleResult.setCredits(3);
    }

    @Test
    void listAll_returnsResults() {
        when(resultRepository.findAll()).thenReturn(List.of(sampleResult));

        var list = resultService.listAll();

        assertEquals(1, list.size());
        assertEquals("CS101", list.get(0).courseCode());
    }

    @Test
    void listByStudent_returnsResultsForStudent() {
        when(resultRepository.findByStudentId(10L)).thenReturn(List.of(sampleResult));

        var list = resultService.listByStudent(10L);

        assertEquals(1, list.size());
        assertEquals(10L, list.get(0).studentId());
    }

    @Test
    void getById_returnsResultWhenFound() {
        when(resultRepository.findById(1L)).thenReturn(Optional.of(sampleResult));

        var response = resultService.getById(1L);

        assertEquals("CS101", response.courseCode());
        assertEquals("A", response.grade());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(resultRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resultService.getById(999L));
    }

    @Test
    void create_savesResultWhenValid() {
        var request = new CreateResultRequest(10L, "CS102", "Fall 2026", "B+", 85.0, 4);
        when(resultRepository.existsByStudentIdAndCourseCodeAndTerm(10L, "CS102", "Fall 2026")).thenReturn(false);
        when(resultRepository.save(any(Result.class))).thenAnswer(inv -> {
            var result = inv.getArgument(0, Result.class);
            result.setId(42L);
            return result;
        });

        var response = resultService.create(request, "lecturer@uni.ms");

        assertEquals(42L, response.id());
        assertEquals("CS102", response.courseCode());
        verify(auditService).record(eq("lecturer@uni.ms"), eq("RESULT_CREATED"), any(String.class));
    }

    @Test
    void create_throwsWhenDuplicate() {
        var request = new CreateResultRequest(10L, "CS101", "Spring 2026", "A", 92.5, 3);
        when(resultRepository.existsByStudentIdAndCourseCodeAndTerm(10L, "CS101", "Spring 2026")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> resultService.create(request, "lecturer@uni.ms"));

        assertEquals("Result already exists for this student, course, and term", ex.getMessage());
    }

    @Test
    void update_modifiesResultWhenFound() {
        when(resultRepository.findById(1L)).thenReturn(Optional.of(sampleResult));
        when(resultRepository.existsByStudentIdAndCourseCodeAndTerm(10L, "CS101", "Spring 2026")).thenReturn(false);
        when(resultRepository.save(any(Result.class))).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateResultRequest("CS101", "Spring 2026", "A+", 95.0, 3);
        var response = resultService.update(1L, request, "lecturer@uni.ms");

        assertEquals("A+", response.grade());
        assertEquals(95.0, response.score());
        verify(auditService).record(eq("lecturer@uni.ms"), eq("RESULT_UPDATED"), any(String.class));
    }

    @Test
    void update_throwsWhenNotFound() {
        when(resultRepository.findById(anyLong())).thenReturn(Optional.empty());

        var request = new UpdateResultRequest("CS101", "Spring 2026", "A", 92.5, 3);

        assertThrows(ResourceNotFoundException.class, () -> resultService.update(999L, request, "lecturer@uni.ms"));
    }

    @Test
    void delete_removesResultWhenFound() {
        when(resultRepository.findById(1L)).thenReturn(Optional.of(sampleResult));

        resultService.delete(1L, "lecturer@uni.ms");

        verify(resultRepository).delete(sampleResult);
        verify(auditService).record(eq("lecturer@uni.ms"), eq("RESULT_DELETED"), any(String.class));
    }

    @Test
    void delete_throwsWhenNotFound() {
        when(resultRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> resultService.delete(999L, "lecturer@uni.ms"));

        verify(resultRepository, never()).delete(any());
    }
}
