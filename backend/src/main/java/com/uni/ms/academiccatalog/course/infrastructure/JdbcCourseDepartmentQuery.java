package com.uni.ms.academiccatalog.course.infrastructure;

import com.uni.ms.academiccatalog.course.application.CourseDepartmentQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcCourseDepartmentQuery implements CourseDepartmentQuery {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean isAvailable(Long departmentId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM departments "
                        + "WHERE id = ? AND deleted_at IS NULL AND status = 'ACTIVE'",
                Long.class, departmentId);
        return count != null && count > 0;
    }
}
