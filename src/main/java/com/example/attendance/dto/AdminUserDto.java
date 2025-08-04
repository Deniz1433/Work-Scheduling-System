package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String password;
    private String roleId;
    private String departmentId;
    private String role;
    private String department;
} 