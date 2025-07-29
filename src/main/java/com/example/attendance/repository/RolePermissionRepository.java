package com.example.attendance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.attendance.model.RolePermission;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
      List<RolePermission> findByRoleId(Long roleId);
}
