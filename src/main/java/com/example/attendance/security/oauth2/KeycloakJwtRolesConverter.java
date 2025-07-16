// src/main/java/com/example/attendance/security/oauth2/KeycloakJwtRolesConverter.java
package com.example.attendance.security.oauth2;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class KeycloakJwtRolesConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1) realm_access.roles → ROLE_realm_{role}
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> realmRoles = (List<String>) realmAccess.get("roles");
            authorities.addAll(
                    realmRoles.stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_realm_" + r))
                            .collect(Collectors.toList())
            );
        }

        // 2) resource_access.attendance-client.roles → ROLE_attendance_client_{role}
        @SuppressWarnings("unchecked")
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.forEach((resource, access) -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) access;
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) m.get("roles");
                if (roles != null) {
                    // replace hyphens so Spring’s hasRole() works cleanly
                    String prefix = resource.replace('-', '_');
                    authorities.addAll(
                            roles.stream()
                                    .map(r -> new SimpleGrantedAuthority("ROLE_" + prefix + "_" + r))
                                    .collect(Collectors.toList())
                    );
                }
            });
        }

        return authorities;
    }
}
