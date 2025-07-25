// src/main/java/com/example/attendance/dto/CreateUserDto.java
package com.example.attendance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserDto {
    @NotBlank private String name;
    @NotBlank private String surname;
    @Email    private String email;
    @NotBlank private String password;
    private String role;
    private String department;
}
