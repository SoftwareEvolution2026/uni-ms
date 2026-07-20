package com.uni.ms.academiccatalog.department.api;

import com.uni.ms.academiccatalog.department.domain.DepartmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank @Size(max = 150)
        @Schema(example = "Computer Science")
        String departmentName,

        @NotBlank @Size(max = 30)
        @Pattern(regexp = "^\\s*[A-Za-z0-9][A-Za-z0-9._-]*\\s*$",
                message = "must contain only letters, numbers, dots, underscores or hyphens")
        @Schema(example = "CS")
        String departmentCode,

        @NotBlank @Size(max = 150)
        @Schema(example = "Faculty of Science")
        String faculty,

        @Size(max = 1000)
        String description,

        @Schema(description = "Defaults to ACTIVE when omitted")
        DepartmentStatus status
) {
}
