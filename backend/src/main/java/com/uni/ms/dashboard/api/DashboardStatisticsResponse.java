package com.uni.ms.dashboard.api;

public record DashboardStatisticsResponse(
        long totalDepartments,
        long activeDepartments,
        long inactiveDepartments,
        long totalCourses,
        long activeCourses,
        long inactiveCourses
) {
}
