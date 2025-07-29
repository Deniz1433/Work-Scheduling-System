// src/main/java/com/example/attendance/service/AdminService.java
package com.example.attendance.service;

import com.example.attendance.dto.AdminUserDto;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AdminService, Keycloak ile kullanıcı ve rol yönetimi işlemlerini gerçekleştirir.
 */
@Service
public class AdminService {
    private final Keycloak kc;
    private final String realm;

    /**
     * AdminService constructor. Keycloak client ve realm bilgisini alır.
     */
    public AdminService(Keycloak keycloakAdminClient,
                        @Value("${keycloak.realm}") String realm) {
        this.kc = keycloakAdminClient;
        this.realm = realm;
    }

    /**
     * İlgili client'ın UUID bilgisini döndürür.
     */
    private String getClientUuid(String clientId) {
        return kc.realm(realm)
                .clients()
                .findByClientId(clientId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Client '" + clientId + "' not found"))
                .getId();
    }

    /**
     * Sistemdeki tüm kullanıcıları listeler.
     */
    public List<UserRepresentation> listAllUsers() {
        return kc.realm(realm)
                .users()
                .list();
    }

    /**
     * Belirtilen client için tüm rolleri listeler.
     */
    public List<RoleRepresentation> listClientRoles(String clientId) {
        String uuid = getClientUuid(clientId);
        return kc.realm(realm)
                .clients()
                .get(uuid)
                .roles()
                .list();
    }

    /**
     * Kullanıcının ilgili client'taki rollerini listeler.
     */
    public List<RoleRepresentation> getUserClientRoles(String userId, String clientId) {
        String uuid = getClientUuid(clientId);
        RoleScopeResource scope = kc.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(uuid);
        return scope.listAll();
    }

    /**
     * Kullanıcıya belirtilen rolleri ekler.
     */
    public void addClientRolesToUser(String userId, String clientId, List<String> rolesToAdd) {
        String uuid = getClientUuid(clientId);
        RoleScopeResource scope = kc.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(uuid);

        var toAdd = listClientRoles(clientId).stream()
                .filter(r -> rolesToAdd.contains(r.getName()))
                .collect(Collectors.toList());
        scope.add(toAdd);
    }

    /**
     * Kullanıcıdan belirtilen rolleri kaldırır.
     */
    public void removeClientRolesFromUser(String userId, String clientId, List<String> rolesToRemove) {
        String uuid = getClientUuid(clientId);
        RoleScopeResource scope = kc.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(uuid);

        var toRemove = listClientRoles(clientId).stream()
                .filter(r -> rolesToRemove.contains(r.getName()))
                .collect(Collectors.toList());
        scope.remove(toRemove);
    }

    /**
     * İlgili client'a yeni bir rol ekler.
     */
    public void createClientRole(String clientId, String roleName) {
        if (clientId == null || clientId.isBlank() || roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("ClientId and roleName must not be null or blank");
        }
        String uuid = getClientUuid(clientId);
        var role = new RoleRepresentation();
        role.setName(roleName);
        role.setClientRole(true);
        kc.realm(realm)
                .clients()
                .get(uuid)
                .roles()
                .create(role);
    }

    /**
     * İlgili client'tan bir rolü siler.
     */
    public void deleteClientRole(String clientId, String roleName) {
        if (clientId == null || clientId.isBlank() || roleName == null || roleName.isBlank()) {
            throw new IllegalArgumentException("ClientId and roleName must not be null or blank");
        }
        String uuid = getClientUuid(clientId);
        kc.realm(realm)
                .clients()
                .get(uuid)
                .roles()
                .deleteRole(roleName);
    }

    /**
     * Tüm admin kullanıcılarını ve rollerini DTO olarak döndürür.
     */
    public List<AdminUserDto> getAllAdminUserDtos(String clientId, List<String> baseRoles) {
        return listAllUsers().stream().map(u -> {
            List<String> roles = getUserClientRoles(u.getId(), clientId)
                .stream()
                .map(RoleRepresentation::getName)
                .toList();
            return new AdminUserDto(u.getId(), u.getUsername(), u.getEmail(), roles);
        }).toList();
    }

    /**
     * Sistemdeki tüm rolleri döndürür.
     */
    public List<RoleRepresentation> getAllRoles(String clientId) {
        return listClientRoles(clientId);
    }
}
