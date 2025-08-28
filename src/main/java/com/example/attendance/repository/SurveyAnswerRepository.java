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
}