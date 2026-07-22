package com.uni.ms.student.repository;

import com.uni.ms.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    Optional<Student> findByStudentNumber(String studentNumber);

    boolean existsByEmail(String email);

    boolean existsByStudentNumber(String studentNumber);

    @Query("SELECT s FROM Student s WHERE " +
            "(:query IS NULL OR :query = '' OR " +
            "LOWER(s.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:department IS NULL OR :department = '' OR " +
            "LOWER(s.department) = LOWER(:department))")
    Page<Student> search(@Param("query") String query,
                         @Param("department") String department,
                         Pageable pageable);
}
