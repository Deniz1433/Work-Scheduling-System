// src/main/java/com/example/attendance/dto/SurveyResultsDto.java
package com.example.attendance.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class SurveyResultsDto {
    private Long id;
    private String title;
    private String description;
    private boolean anonymous;
    private LocalDateTime deadline;

    private List<QuestionResult> questions;

    @Data
    public static class QuestionResult {
        private Long id;
        private String questionText;
        private String type; // "text" | "choice"
        // choice için: seçenek -> adet
        private Map<String, Long> counts;
        // text için: cevap listesi (anonimse email null döneceğiz)
        private List<TextAnswer> texts;
        private Map<String, List<String>> choiceVoters;
        private Long totalCount;
    }

    @Data
    public static class TextAnswer {
        private String answer;
        private String userEmail; // anonymous=true ise null
    }
}