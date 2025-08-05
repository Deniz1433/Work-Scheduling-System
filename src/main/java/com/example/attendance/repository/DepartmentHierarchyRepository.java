// src/main/java/com/example/attendance/repository/RoleHierarchyRepository.java
package com.example.attendance.repository;

import com.example.attendance.model.DepartmentHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepartmentHierarchyRepository extends JpaRepository<DepartmentHierarchy, Long> {
    List<DepartmentHierarchy> findByParentRole(String parentRole);
    List<DepartmentHierarchy> findByChildRole(String childRole);
    void deleteByParentRoleAndChildRole(String parentRole, String childRole);
}
