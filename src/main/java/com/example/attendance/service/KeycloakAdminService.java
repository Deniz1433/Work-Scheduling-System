package com.example.attendance.service;

import com.example.attendance.dto.CreateUserDto;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KeycloakAdminService {

    @Autowired
    private Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public String createKeycloakUser(CreateUserDto dto) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEnabled(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(dto.getPassword());
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));

        var response = keycloak.realm(realm)
                .users()
                .create(user);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Keycloak'ta kullanıcı oluşturulamadı. Status: " + response.getStatus());
        }

        // ID’yi çek
        return keycloak.realm(realm)
                .users()
                .search(dto.getUsername())
                .get(0)
                .getId();
    }
}
