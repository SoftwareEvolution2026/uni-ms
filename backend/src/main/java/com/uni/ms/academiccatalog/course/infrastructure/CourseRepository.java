package com.uni.ms.academiccatalog.course.infrastructure;

import com.uni.ms.academiccatalog.course.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long>,
        JpaSpecificationExecutor<Course> {

    boolean existsByCourseCodeIgnoreCase(String courseCode);

    boolean existsByCourseCodeIgnoreCaseAndIdNot(String courseCode, Long id);

    Optional<Course> findByIdAndDeletedAtIsNull(Long id);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndStatus(com.uni.ms.academiccatalog.course.domain.CourseStatus status);
}
