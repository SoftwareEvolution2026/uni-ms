package com.uni.ms.academiccatalog.course.domain;

import com.uni.ms.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "courses")
@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseEntity {

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "course_name", nullable = false, length = 150)
    private String courseName;

    @Column(name = "course_code", nullable = false, unique = true, length = 30)
    private String courseCode;

    @Column(name = "credit_units", nullable = false)
    private int creditUnits;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Semester semester;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CourseStatus status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static Course create(Long departmentId, String courseName, String courseCode,
                                int creditUnits, Semester semester, String academicYear,
                                String description, CourseStatus status) {
        Course course = new Course();
        course.apply(departmentId, courseName, courseCode, creditUnits, semester,
                academicYear, description, status == null ? CourseStatus.ACTIVE : status);
        return course;
    }

    public void update(Long departmentId, String courseName, String courseCode,
                       int creditUnits, Semester semester, String academicYear,
                       String description, CourseStatus status) {
        if (isDeleted()) {
            throw new IllegalStateException("A deleted course cannot be updated");
        }
        apply(departmentId, courseName, courseCode, creditUnits, semester,
                academicYear, description, status);
    }

    public void softDelete(Instant now) {
        if (isDeleted()) {
            throw new IllegalStateException("Course is already deleted");
        }
        deletedAt = now;
    }

    public void restore() {
        if (!isDeleted()) {
            throw new IllegalStateException("Course is not deleted");
        }
        deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private void apply(Long departmentId, String courseName, String courseCode,
                       int creditUnits, Semester semester, String academicYear,
                       String description, CourseStatus status) {
        this.departmentId = departmentId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.creditUnits = creditUnits;
        this.semester = semester;
        this.academicYear = academicYear;
        this.description = description;
        this.status = status;
    }
}
