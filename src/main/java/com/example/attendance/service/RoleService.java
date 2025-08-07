package com.example.attendance.service;

import com.example.attendance.model.Role;
import com.example.attendance.repository.RoleRepository;
import com.example.attendance.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public RoleService(RoleRepository roleRepository, RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public Role createRole(Role role) {
        if (roleRepository.existsByName(role.getName())) {
            throw new RuntimeException("Bu rol adı zaten mevcut: " + role.getName());
        }
        return roleRepository.save(role);
    }

    @Transactional
    public Role updateRole(Long id, Role role) {
        Role existingRole = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol bulunamadı: " + id));

        if (!existingRole.getName().equals(role.getName()) &&
                roleRepository.existsByName(role.getName())) {
            throw new RuntimeException("Bu rol adı zaten mevcut: " + role.getName());
        }

        existingRole.setName(role.getName());
        existingRole.setDescription(role.getDescription());
        return roleRepository.save(existingRole);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role existingRole = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol bulunamadı: " + id));

        // Önce rolün yetkilerini sil (RolePermission tablosundan)
        rolePermissionRepository.deleteAll(rolePermissionRepository.findByRoleId(id));

        // Sonra rolü sil
        roleRepository.delete(existingRole);
    }
}