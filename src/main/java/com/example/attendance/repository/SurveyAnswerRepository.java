package com.example.attendance.repository;

import com.example.attendance.model.SurveyAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SurveyAnswerRepository extends JpaRepository<SurveyAnswer, Long> {

    boolean existsBySurveyIdAndUserId(Long surveyId, String userId);

    // Bu survey için bu kullanıcının tüm cevapları (myAnswers'ı doldurmak için)
    List<SurveyAnswer> findAllBySurveyIdAndUserId(Long surveyId, String userId);

    // Kullanıcının cevapladığı surveyId'leri tek seferde çek (N+1 önler)
    @Query("select distinct a.survey.id from SurveyAnswer a where a.userId = :userId")
    List<Long> findAnsweredSurveyIds(@Param("userId") String userId);

    // Alternatif: tüm cevaplarını tek seferde çekip servicede gruplarsın
    @Query("select a from SurveyAnswer a where a.userId = :userId")
    List<SurveyAnswer> findAllByUserId(@Param("userId") String userId);

    List<SurveyAnswer> findAllBySurveyId(Long surveyId);

    interface ChoiceAgg {
        Long getQuestionId();
        String getAnswer();
        long getCnt();
    }

    @Query("""
    select sa.questionId as questionId,
           sa.answer as answer,
           count(sa) as cnt
    from SurveyAnswer sa
    where sa.survey.id = :surveyId
    group by sa.questionId, sa.answer
""")
    List<ChoiceAgg> countByQuestionAndAnswer(Long surveyId);
    @Query("""
  select a.userEmail
  from SurveyAnswer a
  where a.survey.id = :surveyId
    and a.questionId = :questionId
    and a.answer = :answer
    and a.userEmail is not null
""")
    List<String> findEmailsBySurveyQuestionAndAnswer(@Param("surveyId") Long surveyId,
                                                     @Param("questionId") Long questionId,
                                                     @Param("answer") String answer);
}