package com.example.attendance.controller;

import com.example.attendance.model.Permission;
import com.example.attendance.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@CrossOrigin(origins = "*")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<Permission>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_ROLES'})")
    @PostMapping
    public ResponseEntity<Permission> createPermission(@RequestBody Permission permission) {
        return ResponseEntity.ok(permissionService.createPermission(permission));
    }

    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_ROLES'})")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}