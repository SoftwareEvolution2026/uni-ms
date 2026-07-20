package com.uni.ms.academiccatalog.department.domain;

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
@Table(name = "departments")
@Getter
@Setter(AccessLevel.NONE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department extends BaseEntity {

    @Column(name = "department_name", nullable = false, length = 150)
    private String departmentName;

    @Column(name = "department_code", nullable = false, unique = true, length = 30)
    private String departmentCode;

    @Column(nullable = false, length = 150)
    private String faculty;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepartmentStatus status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static Department create(String departmentName, String departmentCode,
                                    String faculty, String description,
                                    DepartmentStatus status) {
        Department department = new Department();
        department.apply(departmentName, departmentCode, faculty, description,
                status == null ? DepartmentStatus.ACTIVE : status);
        return department;
    }

    public void update(String departmentName, String departmentCode, String faculty,
                       String description, DepartmentStatus status) {
        if (deletedAt != null) {
            throw new IllegalStateException("A deleted department cannot be updated");
        }
        apply(departmentName, departmentCode, faculty, description, status);
    }

    public void softDelete(Instant deletedAt) {
        if (this.deletedAt != null) {
            throw new IllegalStateException("Department is already deleted");
        }
        this.deletedAt = deletedAt;
    }

    public void restore() {
        if (deletedAt == null) {
            throw new IllegalStateException("Department is not deleted");
        }
        deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    private void apply(String departmentName, String departmentCode, String faculty,
                       String description, DepartmentStatus status) {
        this.departmentName = departmentName;
        this.departmentCode = departmentCode;
        this.faculty = faculty;
        this.description = description;
        this.status = status;
    }
}
