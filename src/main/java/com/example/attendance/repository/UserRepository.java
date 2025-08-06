package com.example.attendance.repository;

import com.example.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKeycloakId(String keycloakId);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByRoleId(Long roleId);
    List<User> findByDepartmentId(Long departmentId);
    List<User> findByIsActive(Boolean isActive);
    void deleteByKeycloakId(String keycloackId);
}