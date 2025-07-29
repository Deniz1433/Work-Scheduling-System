package com.example.attendance.controller;

import com.example.attendance.dto.KeycloakUserCreateRequest;
import com.example.attendance.service.KeycloakAdminService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final KeycloakAdminService keycloakService;

    @Value("${keycloak.realm}")
    private String realm;

    private final org.keycloak.admin.client.Keycloak keycloak;

    @PostMapping
    public String createKeycloakUser(@RequestBody KeycloakUserCreateRequest request) {
        // 1. User Representation
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getName());
        user.setLastName(request.getSurname());

        // 🔧 position ve department -> ATTRIBUTE olarak girilir
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("position", List.of(request.getPosition()));
        attributes.put("department", List.of(request.getDepartment()));
        user.setAttributes(attributes);

        // 2. Kullanıcıyı oluştur
        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(user);
        if (response.getStatus() != 201) {
            return "User creation failed: " + response.getStatusInfo().getReasonPhrase();
        }

        // 3. Oluşan kullanıcı ID’sini çek
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        // 4. Şifre ata
        CredentialRepresentation password = new CredentialRepresentation();
        password.setTemporary(false);
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(request.getPassword());
        usersResource.get(userId).resetPassword(password);

        // ✅ 5. Sadece permissionRole ve departmentRoles kısmını role olarak ata
        List<String> rolesToAssign = new ArrayList<>();
        if (request.getPermissionRole() != null) {
            rolesToAssign.add(request.getPermissionRole()); // örn: "admin"
        }
        if (request.getDepartmentRoles() != null) {
            rolesToAssign.addAll(List.of(request.getDepartmentRoles())); // örn: "Java", "Mobilite"
        }
        keycloakService.updateUserClientRoles(userId, rolesToAssign);

        return "User created successfully";
    }
}
