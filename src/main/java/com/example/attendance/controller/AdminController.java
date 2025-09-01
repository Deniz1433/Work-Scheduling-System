package com.example.attendance.controller;

import com.example.attendance.dto.CreateUserDto;
import com.example.attendance.dto.UserDto;
import com.example.attendance.service.AdminService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'VIEW_ALL_USERS'})")
    @GetMapping("/users")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        try {
            List<UserDto> users = adminService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error fetching all users", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'CREATE_USER'})")
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody CreateUserDto dto) {
        try {
            logger.debug("Received user DTO: {}", dto);
            UserDto createdUser = adminService.createUser(dto);
            return ResponseEntity.ok(createdUser);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request to create user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create user", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Kullanıcı oluşturulamadı: " + e.getMessage()));
        }
    }

    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_USER_INFO'})")
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<String> updateUserRole(@PathVariable Long userId, @RequestBody UserDto userDto) {
        try {
            adminService.updateUserRole(userId, userDto.getRoleId());
            return ResponseEntity.ok("Kullanıcı rolü güncellendi");
        } catch (Exception e) {
            logger.error("Failed to update user role for userId: {}", userId, e);
            return ResponseEntity.badRequest().body("Rol güncellenemedi: " + e.getMessage());
        }
    }

    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_USER_INFO'})")
    @PutMapping("/users/{userId}/department")
    public ResponseEntity<String> updateUserDepartment(@PathVariable Long userId, @RequestBody UserDto userDto) {
        try {
            adminService.updateUserDepartment(userId, userDto.getDepartmentId());
            return ResponseEntity.ok("Kullanıcı departmanı güncellendi");
        } catch (Exception e) {
            logger.error("Failed to update user department for userId: {}", userId, e);
            return ResponseEntity.badRequest().body("Departman güncellenemedi: " + e.getMessage());
        }
    }

    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_USER_INFO'})")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId) {
        try {
            try {
                Long longUserId = Long.parseLong(userId);
                UserDto user = adminService.getUserById(longUserId);
                if (user != null && user.getKeycloakId() != null) {
                    adminService.deleteUser(user.getKeycloakId());
                    logger.info("User deleted successfully: {}", userId);
                    return ResponseEntity.ok("Kullanıcı başarıyla silindi");
                } else {
                    logger.warn("User not found: {}", userId);
                    return ResponseEntity.badRequest().body("Kullanıcı bulunamadı");
                }
            } catch (NumberFormatException e) {
                adminService.deleteUser(userId);
                logger.info("User deleted successfully using keycloakId: {}", userId);
                return ResponseEntity.ok("Kullanıcı başarıyla silindi");
            }
        } catch (Exception e) {
            logger.error("Failed to delete user: {}", userId, e);
            return ResponseEntity.badRequest().body("Kullanıcı silinemedi: " + e.getMessage());
        }
    }

    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'VIEW_ALL_USERS'})")
    @PostMapping("/users/sync")
    public ResponseEntity<String> syncUsersFromKeycloak() {
        try {
            adminService.syncUsersFromKeycloak();
            logger.info("Users synchronized successfully from Keycloak");
            return ResponseEntity.ok("Kullanıcılar başarıyla senkronize edildi");
        } catch (Exception e) {
            logger.error("Failed to synchronize users from Keycloak", e);
            return ResponseEntity.badRequest().body("Senkronizasyon başarısız: " + e.getMessage());
        }
    }
}