package com.uni.ms.academiccatalog.department.infrastructure;

import com.uni.ms.academiccatalog.department.application.DepartmentSearchCriteria;
import com.uni.ms.academiccatalog.department.domain.Department;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DepartmentSpecifications {

    private DepartmentSpecifications() {
    }

    public static Specification<Department> matching(DepartmentSearchCriteria criteria) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteria.deleted()
                    ? builder.isNotNull(root.get("deletedAt"))
                    : builder.isNull(root.get("deletedAt")));

            if (criteria.status() != null) {
                predicates.add(builder.equal(root.get("status"), criteria.status()));
            }
            if (criteria.faculty() != null) {
                predicates.add(builder.equal(
                        builder.lower(root.get("faculty")),
                        criteria.faculty().toLowerCase(Locale.ROOT)));
            }
            if (criteria.search() != null) {
                String pattern = "%" + criteria.search().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("departmentName")), pattern),
                        builder.like(builder.lower(root.get("departmentCode")), pattern),
                        builder.like(builder.lower(root.get("faculty")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
