package com.uni.ms.student.service;

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
import com.uni.ms.user.UserDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserDirectory userDirectory;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
        return StudentResponse.from(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> listAll() {
        return studentRepository.findAll().stream()
                .map(StudentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentPageResponse searchAndPaginate(String query, String department,
                                                 int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction sortDir = sortParts.length > 1
                && sortParts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDir, sortField));
        Page<Student> studentPage = studentRepository.search(query, department, pageable);

        List<StudentResponse> content = studentPage.getContent().stream()
                .map(StudentResponse::from)
                .toList();

        return new StudentPageResponse(
                content,
                studentPage.getNumber(),
                studentPage.getSize(),
                studentPage.getTotalElements(),
                studentPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getProfile(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
        return StudentProfileResponse.from(student);
    }

    @Transactional
    public StudentResponse create(CreateStudentRequest request, String actorEmail) {
        if (studentRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered for a student");
        }
        if (studentRepository.existsByStudentNumber(request.studentNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Student number already exists");
        }

        userDirectory.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + request.userId()));

        Student student = new Student();
        student.setStudentNumber(request.studentNumber());
        student.setFullName(request.fullName());
        student.setEmail(request.email());
        student.setDepartment(request.department());
        student.setPhone(request.phone());
        student.setUserId(request.userId());
        studentRepository.save(student);

        auditService.record(actorEmail, "STUDENT_CREATED", "Created student " + request.email());
        return StudentResponse.from(student);
    }

    @Transactional
    public StudentResponse update(Long id, UpdateStudentRequest request, String actorEmail) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));

        boolean emailChanged = !student.getEmail().equalsIgnoreCase(request.email());
        if (emailChanged && studentRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered for a student");
        }

        boolean numberChanged = !student.getStudentNumber().equals(request.studentNumber());
        if (numberChanged && studentRepository.existsByStudentNumber(request.studentNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Student number already exists");
        }

        student.setStudentNumber(request.studentNumber());
        student.setFullName(request.fullName());
        student.setEmail(request.email());
        student.setDepartment(request.department());
        student.setPhone(request.phone());
        studentRepository.save(student);

        auditService.record(actorEmail, "STUDENT_UPDATED", "Updated student " + student.getEmail());
        return StudentResponse.from(student);
    }

    @Transactional
    public void delete(Long id, String actorEmail) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + id));
        studentRepository.delete(student);
        auditService.record(actorEmail, "STUDENT_DELETED", "Deleted student " + student.getEmail());
    }
}
