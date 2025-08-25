// src/main/java/com/example/attendance/dto/SurveyDto.java
package com.example.attendance.dto;

import lombok.Getter; import lombok.Setter;
import java.util.List;

@Getter @Setter
public class SurveyDto {
    private Long id;
    private String title;
    private String description;
    private List<SurveyQuestionDto> questions;
}