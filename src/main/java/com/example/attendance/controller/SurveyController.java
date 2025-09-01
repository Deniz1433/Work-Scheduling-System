// src/main/java/com/example/attendance/controller/SurveyController.java
package com.example.attendance.controller;

import com.example.attendance.dto.SurveyAnswerDto;
import com.example.attendance.dto.SurveyDto;
import com.example.attendance.dto.SurveyResultsDto;
import com.example.attendance.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    // READ: VIEW_SURVEYS
    @PreAuthorize("@CustomAnnotationEvaluator.hasPermission(authentication, null, 'VIEW_SURVEYS')")
    @GetMapping("/{id}")
    public ResponseEntity<SurveyDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(surveyService.findById(id));
    }

    // READ LIST: VIEW_SURVEYS
    @PreAuthorize("@CustomAnnotationEvaluator.hasPermission(authentication, null, 'VIEW_SURVEYS')")
    @GetMapping
    public ResponseEntity<List<SurveyDto>> list(Principal principal) {
        String userId = (principal != null ? principal.getName() : null);
        List<SurveyDto> out = (userId == null || userId.isBlank())
                ? surveyService.findAll()
                : surveyService.findAllWithStatus(userId);
        return ResponseEntity.ok(out);
    }

    // CREATE: MANAGE_SURVEYS
    @PreAuthorize("@CustomAnnotationEvaluator.hasPermission(authentication, null, 'MANAGE_SURVEYS')")
    @PostMapping
    public ResponseEntity<SurveyDto> create(@RequestBody /*@Valid*/ SurveyDto dto) {
        SurveyDto created = surveyService.create(dto);
        return ResponseEntity
                .created(URI.create("/api/surveys/" + created.getId()))
                .body(created);
    }

    // SUBMIT ANSWERS: VIEW_SURVEYS (users need to be able to see/submit)
    @PreAuthorize("@CustomAnnotationEvaluator.hasPermission(authentication, null, 'VIEW_SURVEYS')")
    @PostMapping("/{surveyId}/submit")
    public ResponseEntity<Void> submit(@PathVariable Long surveyId,
                                       @RequestBody SurveyAnswerDto dto,
                                       Principal principal) {
        String userId = (principal != null ? principal.getName() : null); // Keycloak sub (UUID)
        // NOTE: matches "their version" service signature (no Principal param)
        surveyService.submitAnswers(surveyId, dto, userId);
        return ResponseEntity.noContent().build();
    }

    // RESULTS: VIEW_SURVEYS (adjust to MANAGE_SURVEYS if you want only admins)
    @PreAuthorize("@CustomAnnotationEvaluator.hasPermission(authentication, null, 'VIEW_SURVEYS')")
    @GetMapping("/{id}/results")
    public ResponseEntity<SurveyResultsDto> results(@PathVariable Long id) {
        return ResponseEntity.ok(surveyService.getResults(id));
    }

    // DELETE: MANAGE_SURVEYS
    @PreAuthorize("@CustomAnnotationEvaluator.hasPermission(authentication, null, 'MANAGE_SURVEYS')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        surveyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Optional: Unique constraint violation -> 409 Conflict
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Void> handleIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(409).build();
    }
}
