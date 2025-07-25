// src/main/java/com/example/attendance/service/KeycloakOidcUserSyncService.java
package com.example.attendance.service;

import com.example.attendance.model.User;
import com.example.attendance.repository.UserRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class KeycloakOidcUserSyncService extends OidcUserService {

    private final UserRepository userRepository;

    public KeycloakOidcUserSyncService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        String keycloakId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String username = oidcUser.getPreferredUsername();
        String name = oidcUser.getGivenName();    // from OIDC claim "given_name"
        String surname = oidcUser.getFamilyName(); // from OIDC claim "family_name"

        userRepository.findByKeycloakId(keycloakId).orElseGet(() -> {
            User user = new User();
            user.setId(UUID.randomUUID());
            user.setKeycloakId(keycloakId);
            user.setEmail(email);
            user.setUsername(username);
            user.setName(name);
            user.setSurname(surname);
            user.setIsActive(true);
            return userRepository.save(user);
        });

        return oidcUser;
    }
}
