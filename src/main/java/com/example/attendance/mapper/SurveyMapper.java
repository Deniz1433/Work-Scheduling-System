package com.example.attendance.mapper;

import com.example.attendance.dto.SurveyQuestionDto;
import com.example.attendance.dto.SurveyDto;
import com.example.attendance.model.SurveyQuestion;
import com.example.attendance.model.Survey;

import java.util.List;
import java.util.stream.Collectors;
public class SurveyMapper {
    public static SurveyDto toDto(Survey s) {
        SurveyDto dto = new SurveyDto();
        dto.setId(s.getId());
        dto.setTitle(s.getTitle());
        dto.setDescription(s.getDescription());
        dto.setQuestions(mapQuestions(s.getQuestions()));
        return dto;
    }
    private static List<SurveyQuestionDto> mapQuestions(List<SurveyQuestion> qs) {
        if (qs == null) return List.of();
        return qs.stream().map(q -> {
            SurveyQuestionDto d = new SurveyQuestionDto();
            d.setId(q.getId());
            d.setQuestionText(q.getQuestionText());
            d.setType(q.getType());          // entity’deki alan adına göre uyarlay
            d.setOptions(q.getOptions());    // eğer ayrı tabloda ise uygun şekilde doldur
            return d;
        }).collect(Collectors.toList());
    }
}
