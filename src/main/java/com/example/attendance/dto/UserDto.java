// src/main/java/com/example/attendance/dto/UserDto.java
package com.example.attendance.dto;
import lombok.Data;
import java.util.UUID;

@Data
public class UserDto {
    private UUID id;
    private String name;
    private String surname;
    private String email;
    private String role;
    private String department;
}