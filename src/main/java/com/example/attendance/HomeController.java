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
        testUser.put("id", 1L);
        testUser.put("firstName", "Admin");
        testUser.put("lastName", "User");
        testUser.put("username", "admin");
        testUser.put("departmentId", 1L);
        testUser.put("departmentName", "IT");
        testUser.put("roleId", 1L);
        testUser.put("roleName", "Admin");
        
        log.debug("Returning test user info: {}", testUser);
        return testUser;
        
        /* Orijinal kod - şimdilik devre dışı
        if (oidcUser == null) {
            log.warn("No authenticated user found");
            return Collections.emptyMap();
        }

        Map<String, Object> info = new HashMap<>();
        info.put("name", oidcUser.getName());
        info.put("preferredUsername", oidcUser.getPreferredUsername());
        info.put("email", oidcUser.getEmail());
        info.put("authorities", oidcUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        
        // Veritabanındaki User bilgilerini de ekle
        try {
            Optional<User> dbUser = userRepository.findByKeycloakId(oidcUser.getSubject());
            if (dbUser.isPresent()) {
                User user = dbUser.get();
                info.put("id", user.getId());
                info.put("firstName", user.getFirstName());
                info.put("lastName", user.getLastName());
                info.put("username", user.getUsername());
                if (user.getDepartment() != null) {
                    info.put("departmentId", user.getDepartment().getId());
                    info.put("departmentName", user.getDepartment().getName());
                }
                if (user.getRole() != null) {
                    info.put("roleId", user.getRole().getId());
                    info.put("roleName", user.getRole().getName());
                }
            }
        } catch (Exception e) {
            log.error("Error fetching user from database", e);
            // Hata durumunda bile temel bilgileri döndür
            info.put("error", "Database connection failed");
        }
        
        log.debug("Returning user info: {}", info);
        return info;
        */
    }
}