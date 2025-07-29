package com.example.attendance.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KeycloakUserCreateRequest {
    private String name;
    private String surname;
    private String email;
    private String username;
    private String password;
    private String position;   // Keycloak attribute olarak yazılacak
    private String department; // Keycloak attribute olarak yazılacak
    private String permissionRole; // Örn: "user", "admin", "superadmin"
    private String[] departmentRoles; // Birden fazla olabilir
}
