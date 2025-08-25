// src/main/java/com/example/attendance/model/SurveyQuestion.java
package com.example.attendance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
@Entity @Table(name = "survey_question")
public class SurveyQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;           // örn: “Bu haftaki planlama nasıldı?”

    @Column(nullable = false, length = 50)
    private String type;                   // "text" | "choice"

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "survey_question_options",
            joinColumns = @JoinColumn(name = "survey_question_id")
    )
    @Column(name = "option_value", length = 255)
    private List<String> options;          // type=choice ise doldurulur

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;
}
