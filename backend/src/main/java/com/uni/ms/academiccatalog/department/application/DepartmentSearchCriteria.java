package com.uni.ms.academiccatalog.department.application;

import com.uni.ms.academiccatalog.department.domain.DepartmentStatus;

public record DepartmentSearchCriteria(
        String search,
        DepartmentStatus status,
        String faculty,
        boolean deleted
) {
}
