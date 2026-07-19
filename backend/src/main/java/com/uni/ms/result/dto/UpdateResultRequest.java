package com.uni.ms.result.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateResultRequest(
        @NotBlank(message = "Course code is required")
        String courseCode,
        @NotBlank(message = "Term is required")
        String term,
        @NotBlank(message = "Grade is required")
        String grade,
        @NotNull(message = "Score is required")
        @PositiveOrZero(message = "Score must be non-negative")
        Double score,
        @NotNull(message = "Credits are required")
        @Positive(message = "Credits must be a positive number")
        Integer credits
) {
}
