package com.uni.ms.academiccatalog.department.infrastructure;

import com.uni.ms.academiccatalog.department.application.DepartmentCourseReferenceQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JdbcDepartmentCourseReferenceQuery implements DepartmentCourseReferenceQuery {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public boolean hasAnyCourse(Long departmentId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE department_id = :departmentId",
                new MapSqlParameterSource("departmentId", departmentId), Long.class);
        return count != null && count > 0;
    }

    @Override
    public Map<Long, Long> countByDepartmentIds(Collection<Long> departmentIds) {
        if (departmentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return jdbcTemplate.query(
                "SELECT department_id, COUNT(*) AS course_count FROM courses "
                        + "WHERE department_id IN (:departmentIds) AND deleted_at IS NULL "
                        + "GROUP BY department_id",
                new MapSqlParameterSource("departmentIds", departmentIds),
                (resultSet, rowNumber) -> Map.entry(
                        resultSet.getLong("department_id"),
                        resultSet.getLong("course_count")))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
