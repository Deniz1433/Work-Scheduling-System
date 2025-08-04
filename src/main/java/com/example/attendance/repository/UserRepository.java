package com.example.attendance.repository;

import com.example.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByKeycloakId(String keycloakId);
    List<User> findByFirstNameContainsAndLastNameContainsAndEmailContains(String firstName, String lastName, String email);
}