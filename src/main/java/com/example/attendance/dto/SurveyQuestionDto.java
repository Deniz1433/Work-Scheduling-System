package com.example.attendance.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class SurveyQuestionDto {
    private Long id;
    private String questionText;
    private String type;          // "text" | "choice"
    private List<String> options; // for "choice"
    private boolean multiple;     // NEW: true => checkboxes
}
