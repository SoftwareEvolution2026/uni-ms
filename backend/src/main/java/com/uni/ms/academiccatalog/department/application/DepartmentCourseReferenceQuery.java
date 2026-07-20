package com.uni.ms.academiccatalog.department.application;

import java.util.Collection;
import java.util.Map;

public interface DepartmentCourseReferenceQuery {

    boolean hasAnyCourse(Long departmentId);

    Map<Long, Long> countByDepartmentIds(Collection<Long> departmentIds);
}
