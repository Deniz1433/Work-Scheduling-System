// src/main/java/com/example/attendance/controller/SurveyController.java
package com.example.attendance.controller;

import com.example.attendance.dto.SurveyAnswerDto;
import com.example.attendance.dto.SurveyDto;
import com.example.attendance.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/surveys")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    // VIEW_SURVEYS -> via CustomAnnotationEvaluator (JWT PERM_ or DB role-permission)
    @GetMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@CustomAnnotationEvaluator.hasPermission(authentication, null, 'VIEW_SURVEYS')")
    public ResponseEntity<SurveyDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(surveyService.findById(id));
    }

    @GetMapping
    @org.springframework.security.access.prepost.PreAuthorize("@CustomAnnotationEvaluator.hasPermission(authentication, null, 'VIEW_SURVEYS')")
    public ResponseEntity<List<SurveyDto>> list(Principal principal) {
        String userId = (principal != null ? principal.getName() : null);
        List<SurveyDto> out = (userId == null || userId.isBlank())
                ? surveyService.findAll()
                : surveyService.findAllWithStatus(userId);
        return ResponseEntity.ok(out);
    }

    // MANAGE_SURVEYS -> via CustomAnnotationEvaluator
    @PostMapping
    @org.springframework.security.access.prepost.PreAuthorize("@CustomAnnotationEvaluator.hasPermission(authentication, null, 'MANAGE_SURVEYS')")
    public ResponseEntity<SurveyDto> create(@RequestBody SurveyDto dto) {
        SurveyDto created = surveyService.create(dto);
        return ResponseEntity
                .created(URI.create("/api/surveys/" + created.getId()))
                .body(created);
    }

    @PostMapping("/{surveyId}/submit")
    @org.springframework.security.access.prepost.PreAuthorize("@CustomAnnotationEvaluator.hasPermission(authentication, null, 'VIEW_SURVEYS')")
    public ResponseEntity<Void> submit(@PathVariable Long surveyId,
                                       @RequestBody SurveyAnswerDto dto,
                                       Principal principal) {
        String userId = (principal != null ? principal.getName() : null);
        surveyService.submitAnswers(surveyId, dto, userId, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("@CustomAnnotationEvaluator.hasPermission(authentication, null, 'MANAGE_SURVEYS')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        surveyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
