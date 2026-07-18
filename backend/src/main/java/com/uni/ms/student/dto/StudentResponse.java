package com.uni.ms.student.dto;

import com.uni.ms.student.entity.Student;

public record StudentResponse(
        Long id,
        String studentNumber,
        String fullName,
        String email,
        String department,
        String phone,
        Long userId
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getStudentNumber(),
                student.getFullName(),
                student.getEmail(),
                student.getDepartment(),
                student.getPhone(),
                student.getUserId()
        );
    }
}
