package com.example.attendance.config;

import java.util.*;
import java.util.stream.Collectors;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.util.UriComponentsBuilder;

import com.example.attendance.security.CustomAnnotationEvaluator;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private CustomAnnotationEvaluator customAnnotationEvaluator;

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
                        "/api/userInfo/**"
                ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/static/**","/favicon.ico","/css/**","/js/**","/images/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/", true)
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserServiceWithTokenVerifier()))
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(keycloakLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    /**
     * Maps Keycloak roles/claims to Spring authorities:
     * - Realm roles -> ROLE_realm_{role}
     * - Client roles (attendance-client) -> ROLE_attendance_client_{role}
     * - Optional custom claim 'permissions' -> PERM_{PermissionName}
     * - If client/realm role 'superadmin' present -> grant a bundle of PERM_* (ADMIN_ALL, VIEW_ROLES, EDIT_ROLES, etc.)
     */
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserServiceWithTokenVerifier() {
        OidcUserService delegate = new OidcUserService();
        return userRequest -> {
            String tokenString = userRequest.getAccessToken().getTokenValue();
            Set<GrantedAuthority> mapped = new HashSet<>();

            try {
                AccessToken kcToken = TokenVerifier.create(tokenString, AccessToken.class).getToken();

                // Realm roles -> authorities
                if (kcToken.getRealmAccess() != null) {
                    kcToken.getRealmAccess().getRoles().forEach(
                            r -> mapped.add(new SimpleGrantedAuthority("ROLE_realm_" + r))
                    );
                }

                // Client roles (attendance-client) -> authorities
                Map<String, AccessToken.Access> resources = kcToken.getResourceAccess();
                boolean isSuperadmin = false;
                if (resources != null && resources.containsKey("attendance-client")) {
                    var clientRoles = resources.get("attendance-client").getRoles();
                    for (String r : clientRoles) {
                        String norm = r.replace('-', '_');
                        mapped.add(new SimpleGrantedAuthority("ROLE_attendance_client_" + norm));
                        if ("superadmin".equalsIgnoreCase(r)) {
                            isSuperadmin = true;
                        }
                    }
                }

                // Optional: read custom claim "permissions" (add a protocol mapper in Keycloak!)
                Object rawPerms = kcToken.getOtherClaims().get("permissions");
                if (rawPerms instanceof Collection<?> perms) {
                    for (Object p : perms) {
                        String name = String.valueOf(p).trim();
                        if (!name.isEmpty()) {
                            mapped.add(new SimpleGrantedAuthority("PERM_" + name));
                        }
                    }
                }

                // If they have 'superadmin' client/realm role, grant a bundle of permissions
                if (isSuperadmin
                        || mapped.stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_realm_superadmin"))) {
                    grantSuperadminPermissions(mapped);
                }

            } catch (VerificationException ignored) {
            }

            OidcUser userInfo = delegate.loadUser(userRequest);
            return new DefaultOidcUser(mapped, userInfo.getIdToken(), userInfo.getUserInfo());
        };
    }

    private void grantSuperadminPermissions(Set<GrantedAuthority> mapped) {
        // Grant whatever your app needs to pass the @PreAuthorize checks
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
                "VIEW_DEPARTMENT_HIERARCHY"
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
                    .fromHttpUrl("http://localhost:8081/realms/attendance-realm/protocol/openid-connect/logout")
                    .queryParam("id_token_hint", idTokenHint)
                    .queryParam("post_logout_redirect_uri", redirectUri)
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
