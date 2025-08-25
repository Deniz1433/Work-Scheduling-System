// src/main/java/com/example/attendance/controller/SurveyController.java
package com.example.attendance.controller;

import com.example.attendance.dto.SurveyAnswerDto;
import com.example.attendance.dto.SurveyDto;
import com.example.attendance.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;


import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/surveys")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;

    @GetMapping
    public ResponseEntity<List<SurveyDto>> list() {
        return ResponseEntity.ok(surveyService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SurveyDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(surveyService.findById(id));
    }

    @GetMapping("/latest")
    public ResponseEntity<SurveyDto> latest() {
        return ResponseEntity.ok(surveyService.findLatest());
    }

    //@PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL')")
    @PostMapping
    public ResponseEntity<SurveyDto> create(@RequestBody SurveyDto dto) {
        return ResponseEntity.ok(surveyService.create(dto));
    }

    @PostMapping("/{surveyId}/submit")
    public ResponseEntity<Void> submit(@PathVariable Long surveyId,
                                       @RequestBody SurveyAnswerDto dto,
                                       Principal principal) {
        // Keycloak varsa principal.getName() kullan; yoksa null geç
        String userId = principal != null ? principal.getName() : null;
        surveyService.submitAnswers(surveyId, dto, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        surveyService.delete(id);
        return ResponseEntity.noContent().build();
    }


}