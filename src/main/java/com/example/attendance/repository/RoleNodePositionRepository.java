// src/main/java/com/example/attendance/repository/RoleNodePositionRepository.java
package com.example.attendance.repository;

import com.example.attendance.model.RoleNodePosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleNodePositionRepository extends JpaRepository<RoleNodePosition, String> {
}