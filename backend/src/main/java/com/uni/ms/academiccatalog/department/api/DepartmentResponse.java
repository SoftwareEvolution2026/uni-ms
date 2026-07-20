package com.uni.ms.academiccatalog.department.api;

import com.uni.ms.academiccatalog.department.domain.Department;
import com.uni.ms.academiccatalog.department.domain.DepartmentStatus;

import java.time.Instant;

public record DepartmentResponse(
        Long id,
        String departmentName,
        String departmentCode,
        String faculty,
        String description,
        DepartmentStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        long version,
        long courseCount
) {
    public static DepartmentResponse from(Department department, long courseCount) {
        return new DepartmentResponse(
                department.getId(),
                department.getDepartmentName(),
                department.getDepartmentCode(),
                department.getFaculty(),
                department.getDescription(),
                department.getStatus(),
                department.getCreatedAt(),
                department.getUpdatedAt(),
                department.getDeletedAt(),
                department.getVersion(),
                courseCount
        );
    }
}
