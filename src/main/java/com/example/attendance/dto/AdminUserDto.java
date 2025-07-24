package com.example.attendance.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AdminUserDto {
    private String id;
    private String username;
    private String email;
    private String password;
    private String department_id;
    private String role_id;
    private String name;
    private String surname;
     

    public AdminUserDto(String id, String username, String email, String password, String name, String surname) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.name = name;
        this.surname = surname;
    }
}
