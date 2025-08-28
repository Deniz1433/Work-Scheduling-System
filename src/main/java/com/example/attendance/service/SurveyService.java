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
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyAnswerRepository answerRepository;
    private final UserRepository userRepository;

    public List<SurveyDto> findAll() {
        return surveyRepository.findAll().stream().map(this::toDto).toList();
    }

    public SurveyDto findById(Long id) {
        return surveyRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));
    }

    @Transactional
    public SurveyDto create(SurveyDto dto) {
        Survey survey = new Survey();
        survey.setTitle(dto.getTitle());
        survey.setDescription(dto.getDescription());
        survey.setAnonymous(dto.isAnonymous());
        survey.setDeadline(dto.getDeadline());

        List<SurveyQuestion> qs = (dto.getQuestions() == null ? List.<SurveyQuestionDto>of() : dto.getQuestions())
                .stream()
                .map(q -> {
                    SurveyQuestion sq = new SurveyQuestion();
                    sq.setQuestionText(q.getQuestionText());
                    sq.setType(q.getType());
                    sq.setOptions(q.getOptions());
                    sq.setSurvey(survey);
                    return sq;
                })
                .toList();

        survey.setQuestions(qs);
        return toDto(surveyRepository.save(survey));
    }

    @Transactional
    public void submitAnswers(Long surveyId, SurveyAnswerDto answersDto, String userId, Principal principal) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        LocalDateTime dl = survey.getDeadline();
        if (dl != null && LocalDateTime.now().isAfter(dl)) {
            throw new ResponseStatusException(BAD_REQUEST, "Anketin son giriş tarihi geçti");
        }
        if (survey.getDeadline() != null && LocalDateTime.now().isAfter(survey.getDeadline())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Survey deadline passed");
        }
        // anonim değilse: users tablosundan e-mail’i çek
        final String userEmail = (!survey.isAnonymous() && userId != null)
                ? userRepository.findByKeycloakId(userId).map(u -> u.getEmail()).orElse(null)
                : null;

        answersDto.getAnswers().forEach((qId, ans) -> {
            SurveyAnswer sa = new SurveyAnswer();
            sa.setSurvey(survey);
            sa.setQuestionId(qId);
            sa.setAnswer(ans);
            sa.setUserId(userId);       // UUID (sub)
            sa.setUserEmail(userEmail); // anonim değilse e-mail; anonimse null
            answerRepository.save(sa);
        });
    }

    // --- mapping ---
    private SurveyDto toDto(Survey s) {
        SurveyDto dto = new SurveyDto();
        dto.setId(s.getId());
        dto.setTitle(s.getTitle());
        dto.setDescription(s.getDescription());
        dto.setDeadline(s.getDeadline());
        dto.setAnonymous(s.isAnonymous());

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

    public List<SurveyDto> findAllWithStatus(String userId) {
        // 1) Tüm anketler (id DESC)
        List<Survey> surveys = surveyRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));

        // 2) Kullanıcının yanıtladıkları ve tüm yanıtları tek seferde topla (effectively final)
        final Set<Long> answeredIds =
                (userId != null && !userId.isBlank())
                        ? new HashSet<>(answerRepository.findAnsweredSurveyIds(userId))
                        : Collections.emptySet();

        final Map<Long, Map<Long, String>> myAnswersBySurvey =
                (userId != null && !userId.isBlank())
                        ? answerRepository.findAllByUserId(userId).stream()
                        .collect(Collectors.groupingBy(
                                a -> a.getSurvey().getId(),
                                Collectors.toMap(
                                        SurveyAnswer::getQuestionId,
                                        SurveyAnswer::getAnswer,
                                        (a, b) -> a   // aynı question_id için ilkini al
                                )
                        ))
                        : Collections.emptyMap();

        // 3) DTO'ya map et + alreadyAnswered/myAnswers doldur; 4) stream içinde sırala
        return surveys.stream()
                .map(s -> {
                    SurveyDto dto = toDto(s);
                    boolean answered = answeredIds.contains(s.getId());
                    dto.setAlreadyAnswered(answered);
                    if (answered) {
                        dto.setMyAnswers(myAnswersBySurvey.getOrDefault(s.getId(), Map.of()));
                    }
                    return dto;
                })
                .sorted(Comparator
                        .comparing(SurveyDto::isAlreadyAnswered)                 // false (cevaplanmamış) üstte
                        .thenComparing(SurveyDto::getId, Comparator.reverseOrder())) // sonra id DESC
                .toList();
    }
}