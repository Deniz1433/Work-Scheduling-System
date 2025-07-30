package com.example.attendance.service;

import com.example.attendance.model.Role;
import com.example.attendance.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleService {
    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
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
        if (!roleRepository.existsById(id)) {
            throw new RuntimeException("Rol bulunamadı: " + id);
        }
        roleRepository.deleteById(id);
    }
} 