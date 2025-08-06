package com.example.attendance.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    /**
     * GET /api/user
     * Returns basic info about the logged-in user.
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String,Object>> getUser(@AuthenticationPrincipal OidcUser user) {
        if (user == null) {
            System.out.println("No user info found");
            return null;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("name", user.getName());
        info.put("preferredUsername", user.getPreferredUsername());
        info.put("email", user.getEmail());
        info.put("keycloakId", user.getSubject()); // This is the Keycloak ID
        info.put("authorities", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
                System.out.println("Returning user info:");
                System.out.println(info);
        return ResponseEntity.ok(info);
    }
}