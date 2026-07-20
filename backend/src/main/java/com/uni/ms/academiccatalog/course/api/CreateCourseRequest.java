package com.uni.ms.academiccatalog.course.api;

import com.uni.ms.academiccatalog.course.domain.CourseStatus;
import com.uni.ms.academiccatalog.course.domain.Semester;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotNull Long departmentId,
        @NotBlank @Size(max = 150) String courseName,
        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^\\s*[A-Za-z0-9][A-Za-z0-9._-]*\\s*$",
                message = "must contain only letters, numbers, dots, underscores or hyphens")
        String courseCode,
        @Min(1) @Max(30) int creditUnits,
        @NotNull Semester semester,
        @NotBlank @Pattern(regexp = "^[0-9]{4}/[0-9]{4}$")
        @Schema(example = "2026/2027") String academicYear,
        @Size(max = 1000) String description,
        @Schema(description = "Defaults to ACTIVE when omitted") CourseStatus status
) {
}
