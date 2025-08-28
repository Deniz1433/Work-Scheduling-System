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

    @GetMapping("/{id}")
    public ResponseEntity<SurveyDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(surveyService.findById(id));
    }
    @GetMapping
    public ResponseEntity<List<SurveyDto>> list(Principal principal) {
        String userId = (principal != null ? principal.getName() : null);
        List<SurveyDto> out = (userId == null || userId.isBlank())
                ? surveyService.findAll()
                : surveyService.findAllWithStatus(userId);
        return ResponseEntity.ok(out);
    }
    //@PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL')")
    @PostMapping
    public ResponseEntity<SurveyDto> create(@RequestBody /*@Valid*/ SurveyDto dto) {
        SurveyDto created = surveyService.create(dto);
        // 201 Created + Location header
        return ResponseEntity
                .created(URI.create("/api/surveys/" + created.getId()))
                .body(created);
    }

    @PostMapping("/{surveyId}/submit")
    public ResponseEntity<Void> submit(@PathVariable Long surveyId,
                                       @RequestBody SurveyAnswerDto dto,
                                       Principal principal) {
        String userId = (principal != null ? principal.getName() : null); // UUID (sub)
        surveyService.submitAnswers(surveyId, dto, userId, principal);    // 👈 principal parametresi
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        surveyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}