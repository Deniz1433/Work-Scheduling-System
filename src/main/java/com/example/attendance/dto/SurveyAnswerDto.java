// src/main/java/com/example/attendance/dto/SurveyAnswerDto.java
package com.example.attendance.dto;

import lombok.Getter; import lombok.Setter;
import java.util.Map;

@Getter @Setter
public class SurveyAnswerDto {
    // questionId -> answer
    private Map<Long, String> answers;
}
