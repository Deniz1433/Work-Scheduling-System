// src/main/java/com/example/attendance/repository/SurveyRepository.java
package com.example.attendance.repository;

import com.example.attendance.model.Survey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyRepository extends JpaRepository<Survey, Long> { }
