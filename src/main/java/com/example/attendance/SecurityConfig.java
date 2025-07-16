// src/main/java/com/example/attendance/SecurityConfig.java
package com.example.attendance;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
@EnableMethodSecurity   // enable @PreAuthorize on controller methods
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           ClientRegistrationRepository clients) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**")
                        .hasAnyAuthority(
                                "ROLE_attendance_client_admin",
                                "ROLE_attendance_client_superadmin"
                        )
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserServiceWithTokenVerifier())
                        )
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clients))
                );

        return http.build();
    }

    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserServiceWithTokenVerifier() {
        OidcUserService delegate = new OidcUserService();

        return userRequest -> {
            // parse the ACCESS token (contains realm_access & resource_access)
            String tokenString = userRequest.getAccessToken().getTokenValue();
            Set<GrantedAuthority> mapped = new HashSet<>();

            try {
                AccessToken kcToken = TokenVerifier.create(tokenString, AccessToken.class)
                        .getToken();

                // realm-level roles
                if (kcToken.getRealmAccess() != null && kcToken.getRealmAccess().getRoles() != null) {
                    kcToken.getRealmAccess().getRoles()
                            .forEach(r -> mapped.add(new SimpleGrantedAuthority("ROLE_realm_" + r)));
                }

                // client-specific roles (only attendance-client)
                Map<String, AccessToken.Access> resources = kcToken.getResourceAccess();
                if (resources != null && resources.containsKey("attendance-client")) {
                    AccessToken.Access resource = resources.get("attendance-client");
                    if (resource.getRoles() != null) {
                        resource.getRoles()
                                .forEach(r -> mapped.add(new SimpleGrantedAuthority(
                                        "ROLE_attendance_client_" + r.replace('-', '_')
                                )));
                    }
                }

            } catch (VerificationException e) {
                // ignore token parsing errors
            }

            // build new OidcUser with only Keycloak roles
            DefaultOidcUser user = new DefaultOidcUser(
                    mapped,
                    delegate.loadUser(userRequest).getIdToken(),
                    delegate.loadUser(userRequest).getUserInfo()
            );
            return user;
        };
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository registrations) {
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(registrations);
        handler.setPostLogoutRedirectUri("{baseUrl}/");
        return handler;
    }
}
