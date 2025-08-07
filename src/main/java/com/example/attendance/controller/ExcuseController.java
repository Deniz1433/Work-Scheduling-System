package com.example.attendance.controller;

import com.example.attendance.dto.ExcusesRequest;
import com.example.attendance.dto.ExcuseUpdateRequest;
import com.example.attendance.model.Excuse;
import com.example.attendance.model.User;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.ExcuseService;
import com.example.attendance.security.CustomAnnotationEvaluator;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

@RestController
@RequestMapping("/api/excuse")
public class ExcuseController {
    private final ExcuseService service;
    private final UserRepository userRepository;
    private final CustomAnnotationEvaluator permissionEvaluator;

    public ExcuseController(ExcuseService service, UserRepository userRepository, CustomAnnotationEvaluator permissionEvaluator) {
        this.service = service;
        this.userRepository = userRepository;
        this.permissionEvaluator = permissionEvaluator;
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
    // Kendi excuse'ını silme - permission kontrolü yok
    @DeleteMapping("/my/{id}")
    public ResponseEntity<?> deleteMyExcuse(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            Long currentUserId = getUserIdFromAuthentication(authentication);
            
            // Önce excuse'u bul ve kendine ait olduğunu kontrol et
            Excuse excuse = service.getExcuseById(id);
            if (excuse == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Sadece kendi excuse'ını silebilir
            if (!excuse.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You can only delete your own excuses"));
            }
            
            // Kendi excuse'ını sil
            service.deleteExcuse(currentUserId, id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error deleting excuse: " + e.getMessage()));
        }
    }

    // Başkasının excuse'ını silme - permission kontrolü var
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        try {
            // Önce excuse'u bul ve hangi kullanıcıya ait olduğunu öğren
            Excuse excuse = service.getExcuseById(id);
            if (excuse == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Yetki kontrolü yap - attendance düzenleme yetkisi ile aynı mantık
            if (!permissionEvaluator.canEditAttendance(authentication, excuse.getUserId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Insufficient permissions to delete this user's excuse"));
            }
            
            // Yetki varsa silme işlemini gerçekleştir
            service.deleteExcuse(excuse.getUserId(), id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error deleting excuse: " + e.getMessage()));
        }
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