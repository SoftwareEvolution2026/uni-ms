package com.uni.ms.result.repository;

import com.uni.ms.result.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByStudentId(Long studentId);

    Optional<Result> findByStudentIdAndCourseCodeAndTerm(Long studentId, String courseCode, String term);

    boolean existsByStudentIdAndCourseCodeAndTerm(Long studentId, String courseCode, String term);
}
