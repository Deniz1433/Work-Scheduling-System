package com.example.attendance.service;

import com.example.attendance.model.Permission;
import com.example.attendance.model.Role;
import com.example.attendance.model.RolePermission;
import com.example.attendance.repository.PermissionRepository;
import com.example.attendance.repository.RolePermissionRepository;
import com.example.attendance.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolePermissionService {

    @Autowired
    private RolePermissionRepository rolePermissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    // Belirli bir rolün tüm izinlerini getir
    public List<RolePermission> getPermissionsByRole(Long roleId) {
        try {
            System.out.println("🔍 Service: Getting permissions for roleId: " + roleId);
            List<RolePermission> result = rolePermissionRepository.findByRoleId(roleId);
            System.out.println("✅ Service: Found " + result.size() + " permissions for roleId: " + roleId);
            return result;
        } catch (Exception e) {
            System.err.println("❌ Service: Error in getPermissionsByRole: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Tek izin ata
    public RolePermission assignPermissionToRole(Long roleId, Long permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Rol bulunamadı"));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Yetki bulunamadı"));

        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);

        return rolePermissionRepository.save(rolePermission);
    }

    // Tüm yetkileri güncelle (öncekileri silip yenilerini ekler)
    public List<RolePermission> updateRolePermissions(Long roleId, List<Long> permissionIds) {
        try {
            System.out.println("🔍 Service: Updating permissions for roleId: " + roleId + " with permissionIds: " + permissionIds);
            
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new RuntimeException("Rol bulunamadı"));
            System.out.println("✅ Service: Found role: " + role.getName());

            // Mevcut tüm izinleri sil
            List<RolePermission> existing = rolePermissionRepository.findByRoleId(roleId);
            System.out.println("🔍 Service: Deleting " + existing.size() + " existing permissions");
            rolePermissionRepository.deleteAll(existing);

            // Yeni izinleri ekle
            List<RolePermission> newRolePermissions = permissionIds.stream().map(pid -> {
                Permission permission = permissionRepository.findById(pid)
                        .orElseThrow(() -> new RuntimeException("Yetki bulunamadı: " + pid));
                RolePermission rp = new RolePermission();
                rp.setRole(role);
                rp.setPermission(permission);
                return rp;
            }).collect(Collectors.toList());

            System.out.println("🔍 Service: Saving " + newRolePermissions.size() + " new permissions");
            List<RolePermission> result = rolePermissionRepository.saveAll(newRolePermissions);
            System.out.println("✅ Service: Successfully updated permissions for roleId: " + roleId);
            return result;
        } catch (Exception e) {
            System.err.println("❌ Service: Error in updateRolePermissions: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    // Tek izin sil
    public void removePermissionFromRole(Long rolePermissionId) {
        rolePermissionRepository.deleteById(rolePermissionId);
    }
}
