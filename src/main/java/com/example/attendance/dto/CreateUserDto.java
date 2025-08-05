package com.example.attendance.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateUserDto {
    private String email;
    private String password;
    private String name;
    private String surname;
    private UUID roleId;
    private UUID departmentId;
}
