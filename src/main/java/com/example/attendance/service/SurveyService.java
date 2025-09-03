// src/main/java/com/example/attendance/service/SurveyService.java
package com.example.attendance.service;

import com.example.attendance.dto.SurveyAnswerDto;
import com.example.attendance.dto.SurveyDto;
import com.example.attendance.dto.SurveyQuestionDto;
import com.example.attendance.dto.SurveyResultsDto;
import com.example.attendance.model.Survey;
import com.example.attendance.model.SurveyAnswer;
import com.example.attendance.model.SurveyQuestion;
import com.example.attendance.model.User;
import com.example.attendance.repository.SurveyAnswerRepository;
import com.example.attendance.repository.SurveyRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyRepository surveyRepository;
    private final SurveyAnswerRepository answerRepository;
    private final UserRepository userRepository;

    /* =========================
       Query (list/get)
       ========================= */
    @Transactional(readOnly = true)
    public List<SurveyDto> findAll(boolean includeExpired) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        return surveyRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .filter(s -> includeExpired || s.getDeadline() == null || !s.getDeadline().isBefore(cutoff))
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SurveyDto findById(Long id) {
        return surveyRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));
    }

    /* =========================
       Create / Delete
       ========================= */
    @Transactional
    public SurveyDto create(SurveyDto dto) {
        // --- Survey alanları + temel validasyonlar ---
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Anket başlığı zorunludur");
        }

        Survey survey = new Survey();
        survey.setTitle(dto.getTitle().trim());
        survey.setDescription(dto.getDescription());
        survey.setAnonymous(dto.isAnonymous());

        // deadline & hideAfter: ikisi de opsiyonel
        survey.setDeadline(dto.getDeadline());     // null olabilir
        survey.setHideAfter(dto.getHideAfter());   // null olabilir

        // İsteğe bağlı kural: hideAfter < deadline ise uyar
        if (survey.getHideAfter() != null && survey.getDeadline() != null
                && survey.getHideAfter().isBefore(survey.getDeadline())) {
            throw new IllegalArgumentException("Gizleme tarihi (hideAfter), son tarihten (deadline) önce olamaz");
        }

        // createdAt: entity'de @PrePersist veya burada set edebilirsin
        // Eğer entity'nde yoksa:
        // survey.setCreatedAt(LocalDateTime.now()); // sistem yerel saati

        // --- Sorular ---
        List<SurveyQuestionDto> qDtos = (dto.getQuestions() == null)
                ? List.of()
                : dto.getQuestions();

        List<SurveyQuestion> qs = qDtos.stream()
                .map(q -> {
                    // ortak kontroller
                    if (q.getQuestionText() == null || q.getQuestionText().trim().isEmpty()) {
                        throw new IllegalArgumentException("Soru metni zorunludur");
                    }
                    if (q.getType() == null) {
                        throw new IllegalArgumentException("Soru türü zorunludur");
                    }

                    String type = q.getType().trim().toLowerCase(Locale.ROOT);

                    SurveyQuestion sq = new SurveyQuestion();
                    sq.setQuestionText(q.getQuestionText().trim());
                    sq.setType(type);               // "text" | "choice"
                    sq.setMultiple(q.isMultiple()); // multiple bayrağını taşı
                    sq.setSurvey(survey);

                    if ("choice".equals(type)) {
                        List<String> opts = Optional.ofNullable(q.getOptions())
                                .orElseGet(List::of)
                                .stream()
                                .map(opt -> opt == null ? "" : opt.trim())
                                .filter(opt -> !opt.isEmpty())
                                .distinct()
                                .toList();

                        if (opts.size() < 2) {
                            throw new IllegalArgumentException("Çoktan seçmeli soru için en az 2 seçenek gerekir");
                        }
                        sq.setOptions(opts);
                    } else {
                        sq.setOptions(List.of());
                        sq.setMultiple(false); // text için anlamsız
                    }

                    return sq;
                })
                .toList();

        survey.setQuestions(qs);

        Survey saved = surveyRepository.save(survey);
        return toDto(saved);
    }


    @Transactional
    public void delete(Long id) {
        Survey survey = surveyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));
        surveyRepository.delete(survey);
    }

    /* =========================
       Submit (tek kullanıcı = tek anket cevabı kuralı DB’de unique ile garanti)
       ========================= */
    @Transactional
    public void submitAnswers(Long surveyId, SurveyAnswerDto answersDto, String userId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        LocalDateTime dl = survey.getDeadline();
        if (dl != null && LocalDateTime.now().isAfter(dl)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Survey deadline passed");
        }

        // anonim değilse users tablosundan e-mail çek; anonimse/null ise email yazma
        final String userEmail = (!survey.isAnonymous() && userId != null)
                ? userRepository.findByKeycloakId(userId).map(User::getEmail).orElse(null)
                : null;

        // Not: (survey_id, user_id[, question_id]) unique constraint'in varsa
        // ikinci gönderimde DataIntegrityViolationException -> 409'a map edebilirsin (Controller advice ile).
        answersDto.getAnswers().forEach((qId, list) -> {
            var qOpt = survey.getQuestions().stream()
                    .filter(q -> Objects.equals(q.getId(), qId))
                    .findFirst();
            if (qOpt.isEmpty()) return;
            var q = qOpt.get();

            List<String> values = Optional.ofNullable(list).orElse(List.of());

            if ("text".equalsIgnoreCase(q.getType())) {
                String only = values.stream().findFirst().orElse("");
                if (only.isBlank()) return;
                SurveyAnswer sa = new SurveyAnswer();
                sa.setSurvey(survey);
                sa.setQuestionId(qId);
                sa.setAnswer(only);
                sa.setUserId(userId);
                sa.setUserEmail(userEmail);
                answerRepository.save(sa);
            } else {
                if (!q.isMultiple()) {
                    String only = values.stream().findFirst().orElse(null);
                    if (only == null) return;
                    SurveyAnswer sa = new SurveyAnswer();
                    sa.setSurvey(survey);
                    sa.setQuestionId(qId);
                    sa.setAnswer(only);
                    sa.setUserId(userId);
                    sa.setUserEmail(userEmail);
                    answerRepository.save(sa);
                } else {
                    values.stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(sv -> !sv.isEmpty())
                            .distinct()
                            .forEach(choice -> {
                                SurveyAnswer sa = new SurveyAnswer();
                                sa.setSurvey(survey);
                                sa.setQuestionId(qId);
                                sa.setAnswer(choice);
                                sa.setUserId(userId);
                                sa.setUserEmail(userEmail);
                                answerRepository.save(sa);
                            });
                }
            }
        });
    }

    /* =========================
       “Hepsi” ekranı (cevaplananlar üstte/alta değil — ihtiyaç olursa sıralama eklenir)
       Kullanıcıya göre alreadyAnswered + myAnswers doldurur.
       ========================= */
    @Transactional(readOnly = true)
    public List<SurveyDto> findAllWithStatus(String userId, boolean includeExpired) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);
        List<Survey> surveys = surveyRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .filter(s -> includeExpired || s.getDeadline() == null || !s.getDeadline().isBefore(cutoff))
                .toList();

        final Set<Long> answeredIds =
                (userId != null && !userId.isBlank())
                        ? new HashSet<>(answerRepository.findAnsweredSurveyIds(userId))
                        : Collections.emptySet();

        final Map<Long, Map<Long, List<String>>> myAnswersBySurvey =
                (userId != null && !userId.isBlank())
                        ? answerRepository.findAllByUserId(userId).stream()
                        .collect(Collectors.groupingBy(
                                a -> a.getSurvey().getId(),
                                Collectors.groupingBy(
                                        SurveyAnswer::getQuestionId,
                                        Collectors.mapping(SurveyAnswer::getAnswer, Collectors.toList())
                                )
                        ))
                        : Collections.emptyMap();

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
                .toList();
    }

    /* =========================
       Sonuçlar (Yol B): Tek endpoint’te gösterime hazır payload
       ========================= */
    @Transactional(readOnly = true)
    public SurveyResultsDto getResults(Long surveyId) {
        Survey s = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        boolean anonymous = s.isAnonymous();

        // 1) Çoktan seçmeli sayımlar (tek SQL ile; varsa)
        Map<Long, Map<String, Long>> choiceCountsByQ =
                answerRepository.countByQuestionAndAnswer(surveyId).stream()
                        .collect(Collectors.groupingBy(
                                SurveyAnswerRepository.ChoiceAgg::getQuestionId,
                                Collectors.toMap(
                                        SurveyAnswerRepository.ChoiceAgg::getAnswer,
                                        SurveyAnswerRepository.ChoiceAgg::getCnt
                                )
                        ));

        // 2) Tüm cevaplar (hem text’ler, hem de voters listesi için)
        List<SurveyAnswer> allAnswers = answerRepository.findAllBySurveyId(surveyId);

        // 3) DTO doldur
        SurveyResultsDto out = new SurveyResultsDto();
        out.setId(s.getId());
        out.setTitle(s.getTitle());
        out.setDescription(s.getDescription());
        out.setAnonymous(anonymous);
        out.setDeadline(s.getDeadline());

        List<SurveyResultsDto.QuestionResult> qResults = s.getQuestions().stream().map(q -> {
            SurveyResultsDto.QuestionResult qr = new SurveyResultsDto.QuestionResult();
            qr.setId(q.getId());
            qr.setQuestionText(q.getQuestionText());
            qr.setType(q.getType());

            // Bu soruya ait tüm cevapları çıkar (tek yerde filtreleyelim)
            List<SurveyAnswer> answersForQ = allAnswers.stream()
                    .filter(a -> Objects.equals(a.getQuestionId(), q.getId()))
                    .toList();

            if ("choice".equalsIgnoreCase(q.getType())) {
                // counts: seçenek -> adet (hazır SQL sonucundan ya da lokalde hesap)
                Map<String, Long> counts = choiceCountsByQ.get(q.getId());
                if (counts == null) {
                    counts = answersForQ.stream()
                            .collect(Collectors.groupingBy(
                                    SurveyAnswer::getAnswer,
                                    Collectors.counting()
                            ));
                }
                qr.setCounts(counts);

                // choiceVoters: seçenek -> email listesi (anonimde boş bırak)
                if (!anonymous) {
                    Map<String, List<String>> voters = answersForQ.stream()
                            .filter(a -> a.getUserEmail() != null && !a.getUserEmail().isBlank())
                            .collect(Collectors.groupingBy(
                                    SurveyAnswer::getAnswer,
                                    Collectors.mapping(SurveyAnswer::getUserEmail,
                                            Collectors.collectingAndThen(Collectors.toList(), SurveyService::distinctPreserveOrder))
                            ));
                    qr.setChoiceVoters(voters.isEmpty() ? null : voters);
                } else {
                    qr.setChoiceVoters(null);
                }

                // totalCount: toplam oy sayısı
                long total = (counts != null)
                        ? counts.values().stream().mapToLong(Long::longValue).sum()
                        : answersForQ.size();
                qr.setTotalCount(total);

            } else {
                // TEXT: metin yanıtları
                List<SurveyResultsDto.TextAnswer> texts = answersForQ.stream()
                        .map(a -> {
                            SurveyResultsDto.TextAnswer t = new SurveyResultsDto.TextAnswer();
                            t.setAnswer(a.getAnswer());
                            t.setUserEmail(anonymous ? null : a.getUserEmail());
                            return t;
                        })
                        .toList();
                qr.setTexts(texts);
                qr.setTotalCount((long) texts.size());
            }

            return qr;
        }).toList();

        out.setQuestions(qResults);
        return out;
    }
    private static <T> List<T> distinctPreserveOrder(List<T> in) {
        return new ArrayList<>(new LinkedHashSet<>(in));
    }
    /* =========================
       Mapping helpers (Entity -> DTO)
       ========================= */
    private SurveyDto toDto(Survey s) {
        SurveyDto dto = new SurveyDto();
        dto.setId(s.getId());
        dto.setTitle(s.getTitle());
        dto.setDescription(s.getDescription());
        dto.setAnonymous(s.isAnonymous());
        dto.setDeadline(s.getDeadline());
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
        dto.setMultiple(q.isMultiple());
        return dto;
    }
}