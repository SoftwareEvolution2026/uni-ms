package com.uni.ms.dashboard.infrastructure;

import com.uni.ms.dashboard.api.DashboardStatisticsResponse;
import com.uni.ms.dashboard.application.DashboardQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcDashboardQuery implements DashboardQuery {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public DashboardStatisticsResponse loadStatistics() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COUNT(*) FILTER (WHERE entity_type = 'DEPARTMENT') AS total_departments,
                    COUNT(*) FILTER (WHERE entity_type = 'DEPARTMENT' AND status = 'ACTIVE')
                        AS active_departments,
                    COUNT(*) FILTER (WHERE entity_type = 'DEPARTMENT' AND status = 'INACTIVE')
                        AS inactive_departments,
                    COUNT(*) FILTER (WHERE entity_type = 'COURSE') AS total_courses,
                    COUNT(*) FILTER (WHERE entity_type = 'COURSE' AND status = 'ACTIVE')
                        AS active_courses,
                    COUNT(*) FILTER (WHERE entity_type = 'COURSE' AND status = 'INACTIVE')
                        AS inactive_courses
                FROM (
                    SELECT 'DEPARTMENT' AS entity_type, status
                    FROM departments
                    WHERE deleted_at IS NULL
                    UNION ALL
                    SELECT 'COURSE' AS entity_type, status
                    FROM courses
                    WHERE deleted_at IS NULL
                ) registry
                """, (resultSet, rowNumber) -> new DashboardStatisticsResponse(
                resultSet.getLong("total_departments"),
                resultSet.getLong("active_departments"),
                resultSet.getLong("inactive_departments"),
                resultSet.getLong("total_courses"),
                resultSet.getLong("active_courses"),
                resultSet.getLong("inactive_courses")));
    }
}
