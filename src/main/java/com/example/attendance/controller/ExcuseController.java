package com.example.attendance.controller;

import com.example.attendance.dto.ExcusesRequest;
import com.example.attendance.dto.ExcuseUpdateRequest;
import com.example.attendance.model.Excuse;
import com.example.attendance.model.User;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.ExcuseService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RestController
@RequestMapping("/api/excuse")
public class ExcuseController {
    private final ExcuseService service;
    private final UserRepository userRepository;

    public ExcuseController(ExcuseService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        final String keycloakId;
        Object principal = authentication.getPrincipal();

        if (principal instanceof OidcUser) {
            keycloakId = ((OidcUser) principal).getSubject();
        } else if (principal instanceof OAuth2User) {
            keycloakId = ((OAuth2User) principal).getAttribute("sub");
        } else {
            keycloakId = authentication.getName();
        }

        // Keycloak ID'yi kullanarak User tablosundan gerçek user ID'yi bul
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found for keycloak ID: " + keycloakId));
        return user.getId();
    }

    @PostMapping
    public ResponseEntity<?> submit(
        @RequestBody ExcusesRequest req,
        Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        service.submitExcuses(userId, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<Excuse>> list(
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<Excuse> excuses = service.listUserExcuses(userId);
        return ResponseEntity.ok(excuses);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        service.deleteExcuse(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody ExcuseUpdateRequest req,
            Authentication authentication
    ) {
        Long userId = getUserIdFromAuthentication(authentication);
        service.updateExcuse(userId, id, req);
        return ResponseEntity.ok().build();
    }
}