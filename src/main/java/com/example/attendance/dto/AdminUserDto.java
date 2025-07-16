// src/main/java/com/example/attendance/dto/AdminUserDto.java
package com.example.attendance.dto;

import java.util.List;

public class AdminUserDto {
    private final String id;
    private final String username;
    private final String email;
    private final List<String> clientRoles;

    public AdminUserDto(String id, String username, String email, List<String> clientRoles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.clientRoles = clientRoles;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public List<String> getClientRoles() { return clientRoles; }
}
