package com.example.attendance.dto;
import com.example.attendance.model.Role;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleDto {
    private final Long id;
    private final String name;
    private final String description;

    public RoleDto(Role role) {
        this.id = role.getId();
        this.name = role.getName();
        this.description = role.getDescription();
    }
}
