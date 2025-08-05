// UserDto.java
package com.example.attendance.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private String keycloakId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private String password;
    private Long roleId;
    private Long departmentId;
}