package com.uni.ms.academiccatalog.department.api;

import com.uni.ms.academiccatalog.department.domain.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
        @NotBlank @Size(max = 150) String departmentName,

        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^\\s*[A-Za-z0-9][A-Za-z0-9._-]*\\s*$",
                message = "must contain only letters, numbers, dots, underscores or hyphens")
        String departmentCode,

        @NotBlank @Size(max = 150) String faculty,

        @Size(max = 1000) String description,

        @NotNull DepartmentStatus status,

        @NotNull @PositiveOrZero
        @Schema(description = "Current optimistic-lock version returned by the API")
        Long version
) {
}
