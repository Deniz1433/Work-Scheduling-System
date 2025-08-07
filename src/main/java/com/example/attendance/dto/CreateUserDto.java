package com.example.attendance.dto;

import lombok.Data;


@Data
public class CreateUserDto {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Long roleId;
    private Long departmentId;
}
