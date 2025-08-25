// src/main/java/com/example/attendance/model/SurveyAnswer.java
package com.example.attendance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity @Table(name = "survey_answer")
public class SurveyAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "answer", nullable = false, length = 1000)
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(name = "user_id", length = 64)
    private String userId; // Keycloak sub/username koyabilirsin (opsiyonel)
}