package com.uni.ms.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStudentRequest(
        @NotBlank(message = "Student number is required")
        String studentNumber,

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Department is required")
        String department,

        @NotBlank(message = "Phone is required")
        String phone
) {
}
