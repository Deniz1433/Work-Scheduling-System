// src/main/java/com/example/attendance/dto/SurveyQuestionDto.java
package com.example.attendance.dto;

import lombok.Getter; import lombok.Setter;
import java.util.List;

@Getter @Setter
public class SurveyQuestionDto {
    private Long id;
    private String questionText;
    private String type;           // "text" | "choice"
    private List<String> options;  // choice ise dolu
}
