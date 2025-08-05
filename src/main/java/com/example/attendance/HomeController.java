package com.example.attendance;

import com.example.attendance.model.User;
import com.example.attendance.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final UserRepository userRepository;

    public HomeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * GET /api/user
     * Returns basic info about the logged-in user.
     */
    @GetMapping("/user")
    public Map<String, Object> getUser(@AuthenticationPrincipal OidcUser oidcUser) {
        log.debug("getUser called with oidcUser: {}", oidcUser != null ? oidcUser.getName() : "null");
        
        // Geçici olarak test kullanıcısı döndür
        Map<String, Object> testUser = new HashMap<>();
        testUser.put("name", "Test User");
        testUser.put("preferredUsername", "admin");
        testUser.put("email", "admin@gmail.com");
        testUser.put("authorities", Arrays.asList("ROLE_attendance_client_admin"));
        testUser.put("id", "d5478a21-ee0b-400b-bbee-3c155c4a0d56"); // Keycloak ID'si
        testUser.put("firstName", "Admin");
        testUser.put("lastName", "User");
        testUser.put("username", "admin");
        testUser.put("departmentId", 1L);
        testUser.put("departmentName", "IT");
        testUser.put("roleId", 1L);
        testUser.put("roleName", "Admin");
        
        log.debug("Returning test user info: {}", testUser);
        return testUser;
    }
    
    @GetMapping("/test-db")
    public Map<String, Object> testDatabase() {
        try {
            // Veritabanı bağlantısını test et
            long userCount = userRepository.count();
            return Map.of(
                "status", "success",
                "message", "Database connection successful",
                "userCount", userCount
            );
        } catch (Exception e) {
            log.error("Database connection failed", e);
            return Map.of(
                "status", "error",
                "message", "Database connection failed: " + e.getMessage()
            );
        }
    }
}