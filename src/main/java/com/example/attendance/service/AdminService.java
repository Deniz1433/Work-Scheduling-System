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

    /**
     * Helper to fetch the internal UUID for a given clientId in the realm.
     */
    private String getClientUuid(String clientId) {
        List<ClientRepresentation> clients =
                kc.realm(realm)
                        .clients()
                        .findByClientId(clientId);
        if (clients.isEmpty()) {
            throw new IllegalArgumentException(
                    "Client '" + clientId + "' not found in realm '" + realm + "'");
        }
        return clients.get(0).getId();
    }

    public List<UserRepresentation> listAllUsers() {
        return kc.realm(realm).users().list();
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
        RoleScopeResource roleScope = kc.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(uuid);
        return roleScope.listAll();
    }

    public void addClientRolesToUser(String userId, String clientId, List<String> rolesToAdd) {
        String uuid = getClientUuid(clientId);
        RoleScopeResource roleScope = kc.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(uuid);

        List<RoleRepresentation> toAdd = listClientRoles(clientId).stream()
                .filter(r -> rolesToAdd.contains(r.getName()))
                .collect(Collectors.toList());
        roleScope.add(toAdd);
    }

    public void removeClientRolesFromUser(String userId, String clientId, List<String> rolesToRemove) {
        String uuid = getClientUuid(clientId);
        RoleScopeResource roleScope = kc.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(uuid);

        List<RoleRepresentation> toRemove = listClientRoles(clientId).stream()
                .filter(r -> rolesToRemove.contains(r.getName()))
                .collect(Collectors.toList());
        roleScope.remove(toRemove);
    }

    public List<String> listAllClientIds() {
        return kc.realm(realm)
                .clients()
                .findAll()
                .stream()
                .map(ClientRepresentation::getClientId)
                .toList();
    }

}
