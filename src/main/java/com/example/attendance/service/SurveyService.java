// src/main/java/com/example/attendance/service/SurveyService.java
package com.example.attendance.service;

import com.example.attendance.dto.SurveyAnswerDto;
import com.example.attendance.dto.SurveyDto;
import com.example.attendance.dto.SurveyQuestionDto;
import com.example.attendance.model.Survey;
import com.example.attendance.model.SurveyAnswer;
import com.example.attendance.model.SurveyQuestion;
import com.example.attendance.repository.SurveyAnswerRepository;
import com.example.attendance.repository.SurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyAnswerRepository answerRepository;

    public List<SurveyDto> findAll() {
        return surveyRepository.findAll().stream().map(this::toDto).toList();
    }

    public SurveyDto findById(Long id) {
        return surveyRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));
    }

    public SurveyDto findLatest() {
        return surveyRepository.findAll().stream()
                .max(Comparator.comparing(Survey::getId)) // en son eklenen
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional
    public SurveyDto create(SurveyDto dto) {
        Survey survey = new Survey();
        survey.setTitle(dto.getTitle());
        survey.setDescription(dto.getDescription());

        List<SurveyQuestion> qs = dto.getQuestions().stream().map(q -> {
            SurveyQuestion sq = new SurveyQuestion();
            sq.setQuestionText(q.getQuestionText());
            sq.setType(q.getType());
            sq.setOptions(q.getOptions());
            sq.setSurvey(survey);
            return sq;
        }).toList();

        survey.setQuestions(qs);
        return toDto(surveyRepository.save(survey));
    }

    @Transactional
    public void submitAnswers(Long surveyId, SurveyAnswerDto answersDto, String userId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        answersDto.getAnswers().forEach((qId, ans) -> {
            SurveyAnswer sa = new SurveyAnswer();
            sa.setSurvey(survey);
            sa.setQuestionId(qId);
            sa.setAnswer(ans);
            sa.setUserId(userId);
            answerRepository.save(sa);
        });
    }

    // --- mapping ---
    private SurveyDto toDto(Survey s) {
        SurveyDto dto = new SurveyDto();
        dto.setId(s.getId());
        dto.setTitle(s.getTitle());
        dto.setDescription(s.getDescription());
        if (s.getQuestions() != null) {
            dto.setQuestions(s.getQuestions().stream().map(this::toDto).toList());
        }
        return dto;
    }

    private SurveyQuestionDto toDto(SurveyQuestion q) {
        SurveyQuestionDto dto = new SurveyQuestionDto();
        dto.setId(q.getId());
        dto.setQuestionText(q.getQuestionText());
        dto.setType(q.getType());
        dto.setOptions(q.getOptions());
        return dto;
    }

    @Transactional
    public void delete(Long id) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));
        surveyRepository.delete(survey);
    }





}
