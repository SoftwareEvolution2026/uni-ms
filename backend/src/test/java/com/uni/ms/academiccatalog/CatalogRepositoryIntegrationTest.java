package com.uni.ms.academiccatalog;

import com.uni.ms.academiccatalog.course.application.CourseSearchCriteria;
import com.uni.ms.academiccatalog.course.domain.Course;
import com.uni.ms.academiccatalog.course.domain.CourseStatus;
import com.uni.ms.academiccatalog.course.domain.Semester;
import com.uni.ms.academiccatalog.course.infrastructure.CourseRepository;
import com.uni.ms.academiccatalog.course.infrastructure.CourseSpecifications;
import com.uni.ms.academiccatalog.department.application.DepartmentSearchCriteria;
import com.uni.ms.academiccatalog.department.domain.Department;
import com.uni.ms.academiccatalog.department.domain.DepartmentStatus;
import com.uni.ms.academiccatalog.department.infrastructure.DepartmentRepository;
import com.uni.ms.academiccatalog.department.infrastructure.DepartmentSpecifications;
import com.uni.ms.testsupport.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class CatalogRepositoryIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void cleanCatalog() {
        courseRepository.deleteAll();
        departmentRepository.deleteAll();
    }

    @Test
    void departmentSpecificationsFilterAndExcludeTrash() {
        departmentRepository.save(Department.create(
                "Computer Science", "CS", "Science", null, DepartmentStatus.ACTIVE));
        departmentRepository.save(Department.create(
                "Law", "LAW", "Humanities", null, DepartmentStatus.INACTIVE));
        Department deleted = Department.create(
                "Deleted Science", "DEL", "Science", null, DepartmentStatus.ACTIVE);
        deleted.softDelete(Instant.now());
        departmentRepository.saveAndFlush(deleted);

        var criteria = new DepartmentSearchCriteria(
                "science", DepartmentStatus.ACTIVE, "science", false);
        var page = departmentRepository.findAll(DepartmentSpecifications.matching(criteria),
                PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("CS", page.getContent().get(0).getDepartmentCode());
    }

    @Test
    void courseSpecificationsApplyPostgreSqlFiltersAndTrashBoundary() {
        Department department = departmentRepository.saveAndFlush(Department.create(
                "Computer Science", "CS", "Science", null, DepartmentStatus.ACTIVE));
        courseRepository.save(Course.create(department.getId(), "Algorithms", "CS-301", 4,
                Semester.SEMESTER_2, "2026/2027", "Algorithms and structures",
                CourseStatus.ACTIVE));
        Course deleted = Course.create(department.getId(), "Old Course", "CS-OLD", 3,
                Semester.SEMESTER_1, "2025/2026", null, CourseStatus.INACTIVE);
        deleted.softDelete(Instant.now());
        courseRepository.saveAndFlush(deleted);

        var criteria = new CourseSearchCriteria("2026", department.getId(),
                Semester.SEMESTER_2, "2026/2027", CourseStatus.ACTIVE, false);
        var page = courseRepository.findAll(CourseSpecifications.matching(criteria),
                PageRequest.of(0, 10));

        assertEquals(1, page.getTotalElements());
        assertEquals("CS-301", page.getContent().get(0).getCourseCode());
    }

    @Test
    void postgreSqlForeignKeyRestrictsDepartmentDeletion() {
        Department department = departmentRepository.saveAndFlush(Department.create(
                "Engineering", "ENG", "Engineering", null, DepartmentStatus.ACTIVE));
        courseRepository.saveAndFlush(Course.create(department.getId(), "Structures", "ENG-201",
                3, Semester.SEMESTER_1, "2026/2027", null, CourseStatus.ACTIVE));

        assertThrows(DataIntegrityViolationException.class,
                () -> departmentRepository.delete(department));
    }
}
