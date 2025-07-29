package com.example.attendance.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * CreateRoleDto, yeni rol ekleme isteğini taşır.
 */
public class CreateRoleDto {
    @NotBlank(message = "Rol adı boş olamaz.")
    private String roleName;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
} 