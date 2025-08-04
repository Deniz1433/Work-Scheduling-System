package com.example.attendance.repository;

import com.example.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByKeycloakId(String keycloakId);
    List<User> findByDepartmentId(Long departmentId);
}