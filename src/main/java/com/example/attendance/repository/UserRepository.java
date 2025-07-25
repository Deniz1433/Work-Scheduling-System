// src/main/java/com/example/attendance/repository/UserRepository.java
package com.example.attendance.repository;

import com.example.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByKeycloakId(String keycloakId);

    // for admin search
    List<User> findByNameContainsAndSurnameContainsAndEmailContains(
            String name, String surname, String email
    );
}
