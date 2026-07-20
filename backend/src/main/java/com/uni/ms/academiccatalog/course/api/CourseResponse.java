package com.uni.ms.academiccatalog.course.api;

import com.uni.ms.academiccatalog.course.domain.Course;
import com.uni.ms.academiccatalog.course.domain.CourseStatus;
import com.uni.ms.academiccatalog.course.domain.Semester;

import java.time.Instant;

public record CourseResponse(
        Long id,
        Long departmentId,
        String courseName,
        String courseCode,
        int creditUnits,
        Semester semester,
        String academicYear,
        String description,
        CourseStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        long version
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(course.getId(), course.getDepartmentId(),
                course.getCourseName(), course.getCourseCode(), course.getCreditUnits(),
                course.getSemester(), course.getAcademicYear(), course.getDescription(),
                course.getStatus(), course.getCreatedAt(), course.getUpdatedAt(),
                course.getDeletedAt(), course.getVersion());
    }
}
