// UserDto.java
package com.example.attendance.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
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