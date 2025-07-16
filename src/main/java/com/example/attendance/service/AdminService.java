// src/main/java/com/example/attendance/service/AdminService.java
package com.example.attendance.service;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private final Keycloak kc;
    private final String realm;

    public AdminService(Keycloak keycloakAdminClient,
                        @Value("${keycloak.realm}") String realm) {
        this.kc = keycloakAdminClient;
        this.realm = realm;
    }

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

    // <-- This method was missing
    public List<UserRepresentation> listAllUsers() {
        return kc.realm(realm)
                .users()
                .list();
    }

    public List<RoleRepresentation> listClientRoles(String clientId) {
        String uuid = getClientUuid(clientId);
        return kc.realm(realm)
                .clients()
                .get(uuid)
                .roles()
                .list();
    }

    public List<RoleRepresentation> getUserClientRoles(String userId, String clientId) {
        String uuid = getClientUuid(clientId);
        RoleScopeResource scope = kc.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(uuid);
        return scope.listAll();
    }

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

    public void createClientRole(String clientId, String roleName) {
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

    public void deleteClientRole(String clientId, String roleName) {
        String uuid = getClientUuid(clientId);
        kc.realm(realm)
                .clients()
                .get(uuid)
                .roles()
                .get(roleName)
                .remove();
    }
}
