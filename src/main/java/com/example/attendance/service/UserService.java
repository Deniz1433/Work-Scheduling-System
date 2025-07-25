// src/main/java/com/example/attendance/service/UserService.java
package com.example.attendance.service;

import com.example.attendance.dto.CreateUserDto;
import com.example.attendance.dto.UserDto;
import com.example.attendance.model.User;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repo;
    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.realm}")
    private String realm;

    public List<UserDto> search(String name, String surname, String email) {
        return repo.findByNameContainsAndSurnameContainsAndEmailContains(
                        name, surname, email
                ).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public UserDto create(CreateUserDto dto) {
        // 1) Create user in Keycloak
        UserRepresentation kr = new UserRepresentation();
        kr.setUsername(dto.getEmail());
        kr.setEmail(dto.getEmail());
        kr.setFirstName(dto.getName());
        kr.setLastName(dto.getSurname());
        kr.setEnabled(true);

        Response resp = keycloakAdminClient
                .realm(realm)
                .users()
                .create(kr);
        String keycloakId = CreatedResponseUtil.getCreatedId(resp);

        // 2) Set initial password
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(dto.getPassword());
        cred.setTemporary(false);
        keycloakAdminClient
                .realm(realm)
                .users()
                .get(keycloakId)
                .resetPassword(cred);

        // 3) Mirror to local database
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setKeycloakId(keycloakId);
        u.setUsername(dto.getEmail());
        u.setEmail(dto.getEmail());
        u.setName(dto.getName());
        u.setSurname(dto.getSurname());
        u.setIsActive(true);
        User saved = repo.save(u);

        return toDto(saved);
    }

    public void delete(UUID id) {
        repo.findById(id).ifPresent(u -> {
            // try to remove from Keycloak, but don’t fail if it’s already gone
            try {
                keycloakAdminClient.realm(realm)
                        .users()
                        .get(u.getKeycloakId())
                        .remove();
            } catch (RuntimeException e) {
                // log, but swallow: Keycloak didn’t know about them
                //log.warn("Keycloak delete failed (maybe already removed?): {}", e.getMessage());
            }
            // always delete the local record
            repo.deleteById(id);
        });
    }

    private UserDto toDto(User u) {
        UserDto d = new UserDto();
        BeanUtils.copyProperties(u, d);
        return d;
    }
}