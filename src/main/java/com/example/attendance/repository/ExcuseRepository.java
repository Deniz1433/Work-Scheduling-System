// src/main/java/com/example/attendance/repository/ExcuseRepository.java
package com.example.attendance.repository;

import com.example.attendance.model.Excuse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExcuseRepository extends JpaRepository<Excuse, UUID> {
    List<Excuse> findByUser_KeycloakId(String keycloakId);
}