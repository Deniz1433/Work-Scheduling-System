// src/main/java/com/example/attendance/model/SurveyAnswer.java
package com.example.attendance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(
        name = "survey_answer",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"survey_id", "user_id", "question_id"})
        }
)
public class SurveyAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;   // şimdilik bırakıyoruz (hızlı düzeltme)

    @Column(name = "answer", nullable = false, length = 1000)
    private String answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "user_email", length = 255)
    private String userEmail;
}
