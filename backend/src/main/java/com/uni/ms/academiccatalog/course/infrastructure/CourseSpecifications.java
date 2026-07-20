package com.uni.ms.academiccatalog.course.infrastructure;

import com.uni.ms.academiccatalog.course.application.CourseSearchCriteria;
import com.uni.ms.academiccatalog.course.domain.Course;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CourseSpecifications {

    private CourseSpecifications() {
    }

    public static Specification<Course> matching(CourseSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteria.deleted()
                    ? builder.isNotNull(root.get("deletedAt"))
                    : builder.isNull(root.get("deletedAt")));
            if (criteria.departmentId() != null) {
                predicates.add(builder.equal(root.get("departmentId"), criteria.departmentId()));
            }
            if (criteria.semester() != null) {
                predicates.add(builder.equal(root.get("semester"), criteria.semester()));
            }
            if (criteria.academicYear() != null) {
                predicates.add(builder.equal(root.get("academicYear"), criteria.academicYear()));
            }
            if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.search() != null) {
                String pattern = "%" + criteria.search().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("courseName")), pattern),
                        builder.like(builder.lower(root.get("courseCode")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern),
                        builder.like(builder.lower(root.get("academicYear")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
