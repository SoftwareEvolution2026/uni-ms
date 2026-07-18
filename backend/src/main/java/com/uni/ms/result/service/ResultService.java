package com.uni.ms.result.service;

import com.uni.ms.common.audit.AuditService;
import com.uni.ms.common.exception.ApiException;
import com.uni.ms.common.exception.ResourceNotFoundException;
import com.uni.ms.result.dto.CreateResultRequest;
import com.uni.ms.result.dto.ResultResponse;
import com.uni.ms.result.dto.UpdateResultRequest;
import com.uni.ms.result.entity.Result;
import com.uni.ms.result.repository.ResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final ResultRepository resultRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ResultResponse> listAll() {
        return resultRepository.findAll().stream()
                .map(ResultResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResultResponse> listByStudent(Long studentId) {
        return resultRepository.findByStudentId(studentId).stream()
                .map(ResultResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResultResponse getById(Long id) {
        Result result = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found: " + id));
        return ResultResponse.from(result);
    }

    @Transactional
    public ResultResponse create(CreateResultRequest request, String actorEmail) {
        if (resultRepository.existsByStudentIdAndCourseCodeAndTerm(
                request.studentId(), request.courseCode(), request.term())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Result already exists for this student, course, and term");
        }

        Result result = new Result();
        result.setStudentId(request.studentId());
        result.setCourseCode(request.courseCode());
        result.setTerm(request.term());
        result.setGrade(request.grade());
        result.setScore(request.score());
        result.setCredits(request.credits());
        resultRepository.save(result);

        auditService.record(actorEmail, "RESULT_CREATED",
                "Created result for student " + request.studentId()
                        + " course " + request.courseCode() + " term " + request.term());
        return ResultResponse.from(result);
    }

    @Transactional
    public ResultResponse update(Long id, UpdateResultRequest request, String actorEmail) {
        Result result = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found: " + id));

        boolean collision = !result.getCourseCode().equalsIgnoreCase(request.courseCode())
                || !result.getTerm().equalsIgnoreCase(request.term());
        if (collision && resultRepository.existsByStudentIdAndCourseCodeAndTerm(
                result.getStudentId(), request.courseCode(), request.term())) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Another result already exists for this student, course, and term");
        }

        result.setCourseCode(request.courseCode());
        result.setTerm(request.term());
        result.setGrade(request.grade());
        result.setScore(request.score());
        result.setCredits(request.credits());
        resultRepository.save(result);

        auditService.record(actorEmail, "RESULT_UPDATED",
                "Updated result " + id + " for student " + result.getStudentId());
        return ResultResponse.from(result);
    }

    @Transactional
    public void delete(Long id, String actorEmail) {
        Result result = resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found: " + id));
        resultRepository.delete(result);
        auditService.record(actorEmail, "RESULT_DELETED",
                "Deleted result " + id + " for student " + result.getStudentId());
    }
}
