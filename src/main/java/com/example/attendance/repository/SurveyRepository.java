// src/main/java/com/example/attendance/repository/SurveyRepository.java
package com.example.attendance.repository;

import com.example.attendance.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SurveyRepository extends JpaRepository<Survey, Long> {
    Optional<Survey> findTopByOrderByIdDesc();
}
