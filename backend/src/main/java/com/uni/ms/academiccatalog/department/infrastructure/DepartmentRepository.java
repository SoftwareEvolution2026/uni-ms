package com.uni.ms.academiccatalog.department.infrastructure;

import com.uni.ms.academiccatalog.department.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long>,
        JpaSpecificationExecutor<Department> {

    boolean existsByDepartmentCodeIgnoreCase(String departmentCode);

    boolean existsByDepartmentCodeIgnoreCaseAndIdNot(String departmentCode, Long id);

    Optional<Department> findByIdAndDeletedAtIsNull(Long id);
}
