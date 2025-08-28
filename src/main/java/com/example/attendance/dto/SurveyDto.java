// src/main/java/com/example/attendance/dto/SurveyDto.java
package com.example.attendance.dto;

import lombok.Getter; import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter @Setter
public class SurveyDto {
    private Long id;
    private String title;
    private String description;
    private List<SurveyQuestionDto> questions;

    private boolean alreadyAnswered;
    private Map<Long,String> myAnswers;

    private LocalDateTime deadline;
    private boolean anonymous;
}