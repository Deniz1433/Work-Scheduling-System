package com.example.attendance.controller;

import com.example.attendance.dto.AdminUserDto;
import com.example.attendance.service.AdminService;
import org.springframework.http.ResponseEntity;
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
    
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        try {
            List<AdminUserDto> users = adminService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/users")
    public ResponseEntity<String> createUser(@RequestBody AdminUserDto userDto) {
        try {
            adminService.createUser(userDto);
            return ResponseEntity.ok("Kullanıcı başarıyla oluşturuldu");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Kullanıcı oluşturulamadı: " + e.getMessage());
        }
    }
    
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<String> updateUserRole(@PathVariable String userId, @RequestBody AdminUserDto userDto) {
        try {
            adminService.updateUserRole(userId, userDto.getRoleId());
            return ResponseEntity.ok("Kullanıcı rolü güncellendi");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Rol güncellenemedi: " + e.getMessage());
        }
    }
    
    @PutMapping("/users/{userId}/department")
    public ResponseEntity<String> updateUserDepartment(@PathVariable String userId, @RequestBody AdminUserDto userDto) {
        try {
            adminService.updateUserDepartment(userId, userDto.getDepartmentId());
            return ResponseEntity.ok("Kullanıcı departmanı güncellendi");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Departman güncellenemedi: " + e.getMessage());
        }
    }
} 