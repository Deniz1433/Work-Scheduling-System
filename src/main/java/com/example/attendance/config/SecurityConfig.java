// src/main/java/com/example/attendance/config/SecurityConfig.java
package com.example.attendance.config;

import com.example.attendance.security.CustomAnnotationEvaluator;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity()
public class SecurityConfig {

    private final CustomAnnotationEvaluator customAnnotationEvaluator;

    public SecurityConfig(CustomAnnotationEvaluator customAnnotationEvaluator) {
        this.customAnnotationEvaluator = customAnnotationEvaluator;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/attendance/**",
                        "/api/admin/hierarchy/**",
                        "/api/excuse/**",
                        "/api/admin/**",
                        "/api/departments/**",
                        "/api/roles/**",
                        "/api/role-permissions/**",
                        "/api/holidays/**",
                        "/api/user/**",
                        "/api/userInfo/**",
                        "/api/surveys/**"
                ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/manifest.json",
                                "/favicon.ico",
                                "/logo*.png",
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/")
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserServiceWithTokenVerifier()))
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(PathPatternRequestMatcher.withDefaults().matcher("/logout"))
                        .logoutSuccessHandler(keycloakLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    /**
     * OIDC user service that:
     * 1) Reads Keycloak access token
     * 2) Maps realm roles -> ROLE_realm_*
     * 3) Maps attendance-client roles -> ROLE_attendance_client_*
     * 4) Maps custom "permissions" claim -> PERM_*
     * 5) Bridges PERM_* and relevant roles to app-level authorities used by @PreAuthorize:
     *    - PERM_ADMIN_ALL or superadmin role -> VIEW_SURVEYS + MANAGE_SURVEYS
     *    - PERM_VIEW_SURVEYS -> VIEW_SURVEYS
     *    - PERM_MANAGE_SURVEYS -> MANAGE_SURVEYS
     *    - ROLE_attendance_client_view_surveys OR ROLE_realm_view_surveys -> VIEW_SURVEYS
     *    - ROLE_attendance_client_manage_surveys OR ROLE_realm_manage_surveys -> MANAGE_SURVEYS
     */
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserServiceWithTokenVerifier() {
        OidcUserService delegate = new OidcUserService();
        return userRequest -> {
            String tokenString = userRequest.getAccessToken().getTokenValue();
            Set<GrantedAuthority> mapped = new HashSet<>();

            // --- Parse Keycloak access token and collect roles/perms ---
            Set<String> realmRoles = new HashSet<>();
            Set<String> clientRoles = new HashSet<>();
            Set<String> permClaims = new HashSet<>();
            boolean superadmin = false;

            try {
                AccessToken kc = TokenVerifier.create(tokenString, AccessToken.class).getToken();

                if (kc.getRealmAccess() != null) {
                    realmRoles.addAll(kc.getRealmAccess().getRoles());
                }

                Map<String, AccessToken.Access> res = kc.getResourceAccess();
                if (res != null && res.containsKey("attendance-client")) {
                    clientRoles.addAll(res.get("attendance-client").getRoles());
                }

                Object rawPerms = kc.getOtherClaims().get("permissions");
                if (rawPerms instanceof Collection<?> col) {
                    for (Object p : col) {
                        String name = String.valueOf(p).trim();
                        if (!name.isEmpty()) permClaims.add(name);
                    }
                }

            } catch (VerificationException ignored) {
                // Consider logging in production
            }

            // --- Convert roles/claims to GrantedAuthority baseline ---
            for (String r : realmRoles) {
                mapped.add(new SimpleGrantedAuthority("ROLE_realm_" + r));
            }
            for (String r : clientRoles) {
                String norm = r.replace('-', '_');
                mapped.add(new SimpleGrantedAuthority("ROLE_attendance_client_" + norm));
                if ("superadmin".equalsIgnoreCase(r)) superadmin = true;
            }
            for (String p : permClaims) {
                mapped.add(new SimpleGrantedAuthority("PERM_" + p));
            }

            // Superadmin (client or realm) -> grant bundle of PERM_* (includes survey perms)
            if (superadmin || realmRoles.contains("superadmin")) {
                grantSuperadminPermissions(mapped);
            }

            // --- BRIDGE to app-level authorities used by @PreAuthorize ---
            Set<String> auths = mapped.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

            boolean hasAdminAll = auths.contains("PERM_ADMIN_ALL")
                    || auths.contains("ROLE_attendance_client_superadmin")
                    || realmRoles.contains("superadmin");

            boolean hasViewPerm   = auths.contains("PERM_VIEW_SURVEYS");
            boolean hasManagePerm = auths.contains("PERM_MANAGE_SURVEYS");

            boolean hasViewRoleRealm   = auths.contains("ROLE_realm_view_surveys");
            boolean hasViewRoleClient  = auths.contains("ROLE_attendance_client_view_surveys");
            boolean hasManageRoleRealm = auths.contains("ROLE_realm_manage_surveys");
            boolean hasManageRoleClient= auths.contains("ROLE_attendance_client_manage_surveys");

            if (hasAdminAll) {
                mapped.add(new SimpleGrantedAuthority("VIEW_SURVEYS"));
                mapped.add(new SimpleGrantedAuthority("MANAGE_SURVEYS"));
            }
            if (hasViewPerm || hasViewRoleRealm || hasViewRoleClient) {
                mapped.add(new SimpleGrantedAuthority("VIEW_SURVEYS"));
            }
            if (hasManagePerm || hasManageRoleRealm || hasManageRoleClient) {
                mapped.add(new SimpleGrantedAuthority("MANAGE_SURVEYS"));
            }

            // Finally return the OIDC user with our mapped authorities
            OidcUser base = delegate.loadUser(userRequest);
            return new DefaultOidcUser(mapped, base.getIdToken(), base.getUserInfo());
        };
    }

    private void grantSuperadminPermissions(Set<GrantedAuthority> mapped) {
        List<String> perms = List.of(
                "ADMIN_ALL",
                "VIEW_ROLES",
                "EDIT_ROLES",
                "CREATE_ROLE",
                "VIEW_ALL_ATTENDANCE",
                "EDIT_ALL_ATTENDANCE",
                "VIEW_ALL_USERS",
                "VIEW_ALL_DEPARTMENTS",
                "VIEW_HOLIDAYS",
                "VIEW_DEPARTMENT_HIERARCHY",
                // explicitly include survey perms
                "VIEW_SURVEYS",
                "MANAGE_SURVEYS"
        );
        perms.forEach(p -> mapped.add(new SimpleGrantedAuthority("PERM_" + p)));
    }

    private LogoutSuccessHandler keycloakLogoutSuccessHandler() {
        return (request, response, auth) -> {
            String redirectUri = "http://localhost:8080/";
            String idTokenHint = "";

            if (auth != null && auth.getPrincipal() instanceof OidcUser u) {
                idTokenHint = u.getIdToken().getTokenValue();
            }

            String logoutUrl = UriComponentsBuilder
                    .fromUriString("http://localhost:8081/realms/attendance-realm/protocol/openid-connect/logout")
                    .queryParam("id_token_hint", idTokenHint)
                    .queryParam("post_logout_redirect_uri", redirectUri)
                    .build()
                    .toUriString();

            response.sendRedirect(logoutUrl);
        };
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(customAnnotationEvaluator);
        return handler;
    }
}
