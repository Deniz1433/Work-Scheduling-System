package com.example.attendance.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SurveyAnswerDto {
    // questionId -> answers
    // For text and single-choice: single-item list
    // For multi-choice: list of items
    private Map<Long, List<String>> answers;
}
