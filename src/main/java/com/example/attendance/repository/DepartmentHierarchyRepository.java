// src/main/java/com/example/attendance/repository/RoleHierarchyRepository.java
package com.example.attendance.repository;

import com.example.attendance.model.Department;
import com.example.attendance.model.DepartmentHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepartmentHierarchyRepository extends JpaRepository<DepartmentHierarchy, Long> {
    List<DepartmentHierarchy> findByParentDepartment(Department parentDepartment);
    List<DepartmentHierarchy> findByChildDepartment(Department childRole);
    void deleteByParentDepartmentAndChildDepartment(Department parentRole, Department childRole);
}
