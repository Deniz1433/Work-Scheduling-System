// src/main/java/com/example/attendance/repository/RoleHierarchyRepository.java
package com.example.attendance.repository;

import com.example.attendance.model.RoleHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoleHierarchyRepository extends JpaRepository<RoleHierarchy, Long> {
    List<RoleHierarchy> findByParentRole(String parentRole);
    List<RoleHierarchy> findByChildRole(String childRole);
    void deleteByParentRoleAndChildRole(String parentRole, String childRole);
}
