// src/main/java/com/example/attendance/model/Survey.java
package com.example.attendance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter
@Entity @Table(name = "survey")
public class Survey {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name ="anonymous",nullable = false)
    private boolean anonymous=false;

    @OneToMany(mappedBy = "survey", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.EAGER)
    private List<SurveyQuestion> questions;
}