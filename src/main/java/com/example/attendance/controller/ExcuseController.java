package com.example.attendance.controller;

import com.example.attendance.dto.ExcusesRequest;
import com.example.attendance.dto.ExcuseUpdateRequest;
import com.example.attendance.model.Excuse;
import com.example.attendance.service.ExcuseService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RestController
@RequestMapping("/api/excuse")
public class ExcuseController {
    private final ExcuseService service;

    public ExcuseController(ExcuseService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> submit(
        @RequestBody ExcusesRequest req,
        Authentication authentication
    ) {
        String userId = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof OidcUser) {
            userId = ((OidcUser) principal).getSubject(); // OIDC için
        } else if (principal instanceof OAuth2User) {
            userId = ((OAuth2User) principal).getAttribute("sub"); // Sadece OAuth2 için
        } else {
            userId = authentication.getName(); // fallback
        }
        
        service.submitExcuses(userId, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<Excuse>> list(
            Principal principal
    ) {
        List<Excuse> excuses = service.listUserExcuses(principal.getName());
        return ResponseEntity.ok(excuses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String userId = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof OidcUser) {
            userId = ((OidcUser) principal).getSubject(); // OIDC için
        } else if (principal instanceof OAuth2User) {
            userId = ((OAuth2User) principal).getAttribute("sub"); // Sadece OAuth2 için
        } else {
            userId = authentication.getName(); // fallback
        }

        service.deleteExcuse(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody ExcuseUpdateRequest req,
            Authentication authentication
    ) {
        String userId = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof OidcUser) {
            userId = ((OidcUser) principal).getSubject();
        } else if (principal instanceof OAuth2User) {
            userId = ((OAuth2User) principal).getAttribute("sub");
        } else {
            userId = authentication.getName();
        }

        service.updateExcuse(userId, id, req);
        return ResponseEntity.ok().build();
    }
}
