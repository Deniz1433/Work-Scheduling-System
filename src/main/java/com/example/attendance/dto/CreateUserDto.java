package com.example.attendance.dto;

import lombok.Data;


@Data
public class CreateUserDto {
    private String email;
    private String password;
    private String name;
    private String surname;
    private Long roleId;
    private Long departmentId;
}
