// src/main/java/com/example/attendance/repository/RoleHierarchyRepository.java
package com.example.attendance.repository;

import com.example.attendance.model.RoleHierarchy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface RoleHierarchyRepository extends JpaRepository<RoleHierarchy, Long> {
    List<RoleHierarchy> findByParentRole(String parentRole);
    List<RoleHierarchy> findByChildRole(String childRole);
    void deleteByParentRoleAndChildRole(String parentRole, String childRole);

    // Sadece parentRole döndüren projection
    @Query("select r.parentRole from RoleHierarchy r where r.childRole = :childRole")
    List<String> findParentRolesByChildRole(String childRole);

    // Sadece childRole döndüren projection
    @Query("select r.childRole from RoleHierarchy r where r.parentRole = :parentRole")
    List<String> findChildRolesByParentRole(String parentRole);
}
