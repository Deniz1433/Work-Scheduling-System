// src/main/java/com/example/attendance/repository/SurveyAnswerRepository.java
package com.example.attendance.repository;

import com.example.attendance.model.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> { }

