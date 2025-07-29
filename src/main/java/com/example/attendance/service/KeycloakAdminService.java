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
    public void createUserWithAttributes(
            String username,
            String email,
            String password,
            String firstName,
            String lastName,
            String department,
            String position,
            List<String> roles
    ) {
        // 1. Kullanıcı oluştur
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);

        // 2. Attribute'ları ekle
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("department", Collections.singletonList(department));
        attributes.put("position", Collections.singletonList(position));
        user.setAttributes(attributes);

        // 3. Keycloak'a gönder
        var usersResource = keycloak.realm(realm).users();
        var response = usersResource.create(user);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user: " + response.getStatus());
        }

        // 4. Oluşan kullanıcının ID'sini al
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        // 5. Parola ayarla
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        usersResource.get(userId).resetPassword(credential);

        // 6. Roller ekle
        updateUserClientRoles(userId, roles);
    }
}
