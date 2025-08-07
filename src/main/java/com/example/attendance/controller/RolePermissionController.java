package com.example.attendance.controller;

import com.example.attendance.model.RolePermission;
import com.example.attendance.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/role-permissions")
@CrossOrigin(origins = "*")
public class RolePermissionController {

    @Autowired
    private RolePermissionService rolePermissionService;

    // Belirli bir rolün tüm izinlerini getir
    @GetMapping("/{roleId}")
    public ResponseEntity<List<RolePermission>> getPermissionsByRole(@PathVariable Long roleId) {
        return ResponseEntity.ok(rolePermissionService.getPermissionsByRole(roleId));
    }

    // Rol'e tek izin ata (isteğe bağlı kullanılabilir)
    @PostMapping("/{roleId}/assign/{permissionId}")
    public ResponseEntity<RolePermission> assignPermissionToRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        return ResponseEntity.ok(rolePermissionService.assignPermissionToRole(roleId, permissionId));
    }

    // Belirli bir rolün yetkilerini topluca güncelle
    @PutMapping("/{roleId}")
    public ResponseEntity<List<RolePermission>> updateRolePermissions(
            @PathVariable Long roleId,
            @RequestBody List<Long> permissionIds
    ) {
        return ResponseEntity.ok(rolePermissionService.updateRolePermissions(roleId, permissionIds));
    }

    // Rol'den izin sil
    @DeleteMapping("/{rolePermissionId}")
    public ResponseEntity<Void> removePermissionFromRole(@PathVariable Long rolePermissionId) {
        rolePermissionService.removePermissionFromRole(rolePermissionId);
        return ResponseEntity.noContent().build();
    }
}
