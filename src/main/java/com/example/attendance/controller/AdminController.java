package com.example.attendance.controller;

import com.example.attendance.dto.CreateUserDto;
import com.example.attendance.dto.UserDto;
import com.example.attendance.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

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
            return ResponseEntity.internalServerError().build();
        }
    }
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'CREATE_USER'})")
    @PostMapping("/users")
    public ResponseEntity<String> createUser(@RequestBody CreateUserDto dto) {
        try {
            System.out.println("Gelen kullanıcı DTO: " + dto);
            adminService.createUser(dto);
            return ResponseEntity.ok("Kullanıcı başarıyla oluşturuldu.");
        } catch (Exception e) {
            e.printStackTrace(); // Konsola full hata stack'ini basar
            return ResponseEntity.badRequest().body("Kullanıcı oluşturulamadı: " + e.getMessage());
        }
    }
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_USER_INFO'})")
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<String> updateUserRole(@PathVariable Long userId, @RequestBody UserDto userDto) {
        try {
            adminService.updateUserRole(userId, userDto.getRoleId());
            return ResponseEntity.ok("Kullanıcı rolü güncellendi");
        } catch (Exception e) {
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
            return ResponseEntity.badRequest().body("Departman güncellenemedi: " + e.getMessage());
        }
    }

    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_USER_INFO'})")
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable String userId) {
        try {
            // Önce userId'nin Long olup olmadığını kontrol et
            try {
                Long longUserId = Long.parseLong(userId);
                // Eğer Long ise, önce kullanıcıyı bul ve keycloakId'sini al
                UserDto user = adminService.getUserById(longUserId);
                if (user != null && user.getKeycloakId() != null) {
                    adminService.deleteUser(user.getKeycloakId());
                    return ResponseEntity.ok("Kullanıcı başarıyla silindi");
                } else {
                    return ResponseEntity.badRequest().body("Kullanıcı bulunamadı");
                }
            } catch (NumberFormatException e) {
                // Eğer Long değilse, direkt keycloakId olarak kullan
                adminService.deleteUser(userId);
                return ResponseEntity.ok("Kullanıcı başarıyla silindi");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Kullanıcı silinemedi: " + e.getMessage());
        }
    }
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'VIEW_ALL_USERS'})")
    @PostMapping("/users/sync")
    public ResponseEntity<String> syncUsersFromKeycloak() {
        try {
            adminService.syncUsersFromKeycloak();
            return ResponseEntity.ok("Kullanıcılar başarıyla senkronize edildi");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Senkronizasyon başarısız: " + e.getMessage());
        }
    }
}