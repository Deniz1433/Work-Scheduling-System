package com.example.attendance.controller;

import com.example.attendance.model.RolePermission;
import com.example.attendance.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-permissions")
@CrossOrigin(origins = "*")
public class RolePermissionController {

    @Autowired
    private RolePermissionService rolePermissionService;

    // Belirli bir rolün tüm izinlerini getir
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_ROLES'})")
    @GetMapping("/{roleId}")
    public ResponseEntity<List<RolePermission>> getPermissionsByRole(@PathVariable Long roleId) {
        try {
            System.out.println("🔍 GET /api/role-permissions/" + roleId + " called");
            List<RolePermission> result = rolePermissionService.getPermissionsByRole(roleId);
            System.out.println("✅ GET /api/role-permissions/" + roleId + " returned " + result.size() + " permissions");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("❌ Error in getPermissionsByRole: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Rol'e tek izin ata (isteğe bağlı kullanılabilir)
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_ROLES'})")
    @PostMapping("/{roleId}/assign/{permissionId}")
    public ResponseEntity<RolePermission> assignPermissionToRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        return ResponseEntity.ok(rolePermissionService.assignPermissionToRole(roleId, permissionId));
    }

    // Belirli bir rolün yetkilerini topluca güncelle
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_ROLES'})")
    @PutMapping("/{roleId}")
    public ResponseEntity<List<RolePermission>> updateRolePermissions(
            @PathVariable Long roleId,
            @RequestBody List<Long> permissionIds
    ) {
        try {
            System.out.println("🔍 PUT /api/role-permissions/" + roleId + " called with permissions: " + permissionIds);
            List<RolePermission> result = rolePermissionService.updateRolePermissions(roleId, permissionIds);
            System.out.println("✅ PUT /api/role-permissions/" + roleId + " updated successfully with " + result.size() + " permissions");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("❌ Error in updateRolePermissions: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    // Rol'den izin sil
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_ROLES'})")
    @DeleteMapping("/{rolePermissionId}")
    public ResponseEntity<Void> removePermissionFromRole(@PathVariable Long rolePermissionId) {
        rolePermissionService.removePermissionFromRole(rolePermissionId);
        return ResponseEntity.noContent().build();
    }
}
