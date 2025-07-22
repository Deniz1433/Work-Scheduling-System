package com.example.attendance.service;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KeycloakAdminService {

    private static final String CLIENT_ID = "attendance-client";

    @Autowired
    private Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    private String getClientUuid(String clientId) {
        return keycloak.realm(realm)
                .clients()
                .findByClientId(clientId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Client '" + clientId + "' not found"))
                .getId();
    }

    /** Fetch all users in the realm. */
    public List<UserRepresentation> getAllUsers() {
        return keycloak.realm(realm)
                .users()
                .list();
    }

    /** List all roles for a given client. */
    public List<RoleRepresentation> listClientRoles(String clientId) {
        String uuid = getClientUuid(clientId);
        return keycloak.realm(realm)
                .clients()
                .get(uuid)
                .roles()
                .list();
    }

    /** Fetch all roles under attendance-client. */
    public List<RoleRepresentation> getAllClientRoles() {
        return listClientRoles(CLIENT_ID);
    }

    /** The three “permission” roles: user, admin, superadmin. */
    public List<RoleRepresentation> getPermissionRoles() {
        Set<String> perms = Set.of("user", "admin", "superadmin");
        return getAllClientRoles().stream()
                .filter(r -> perms.contains(r.getName()))
                .collect(Collectors.toList());
    }

    /** Everything else is a “department” role. */
    public List<RoleRepresentation> getDepartmentRoles() {
        Set<String> perms = Set.of("user", "admin", "superadmin");
        return getAllClientRoles().stream()
                .filter(r -> !perms.contains(r.getName()))
                .collect(Collectors.toList());
    }

    /** Get a user’s RoleRepresentation list for any client. */
    public List<RoleRepresentation> getUserClientRoles(String userId, String clientId) {
        String uuid = getClientUuid(clientId);
        RoleScopeResource scope = keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(uuid);
        return scope.listAll();
    }

    /** Alias used by the controller: just get names under attendance-client. */
    public List<String> getUserClientRoles(String userId) {
        return keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(getClientUuid(CLIENT_ID))
                .listEffective()
                .stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    /** Replace a user’s attendance-client roles in one call. */
    public void updateUserClientRoles(String userId, List<String> newRoles) {
        String uuid = getClientUuid(CLIENT_ID);
        RoleScopeResource scope = keycloak.realm(realm)
                .users()
                .get(userId)
                .roles()
                .clientLevel(uuid);

        // remove all then add selected
        List<RoleRepresentation> allReps = getAllClientRoles();
        scope.remove(allReps);
        List<RoleRepresentation> toAssign = allReps.stream()
                .filter(r -> newRoles.contains(r.getName()))
                .collect(Collectors.toList());
        scope.add(toAssign);
    }

    /** Create a new client-role under attendance-client. */
    public void createClientRole(String roleName) {
        String uuid = getClientUuid(CLIENT_ID);
        RoleRepresentation rep = new RoleRepresentation();
        rep.setName(roleName);
        rep.setClientRole(true);
        keycloak.realm(realm)
                .clients()
                .get(uuid)
                .roles()
                .create(rep);
    }

    /** Delete a client-role under attendance-client. */
    public void deleteClientRole(String roleName) {
        String uuid = getClientUuid(CLIENT_ID);
        keycloak.realm(realm)
                .clients()
                .get(uuid)
                .roles()
                .get(roleName)
                .remove();
    }
}
