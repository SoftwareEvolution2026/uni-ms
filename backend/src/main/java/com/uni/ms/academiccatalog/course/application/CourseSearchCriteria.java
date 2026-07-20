package com.uni.ms.academiccatalog.course.application;

import com.uni.ms.academiccatalog.course.domain.CourseStatus;
import com.uni.ms.academiccatalog.course.domain.Semester;

public record CourseSearchCriteria(
        String search,
        Long departmentId,
        Semester semester,
        String academicYear,
        CourseStatus status,
        boolean deleted
) {
}
