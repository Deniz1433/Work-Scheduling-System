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
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // disable CSRF for our stateless API endpoints
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/attendance/**", "/api/excuse/**", "/api/admin/**")
                )
                .authorizeHttpRequests(auth -> auth
                        // allow React static assets
                        .requestMatchers(
                                "/static/**",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // UI entrypoints under /admin/**
                        .requestMatchers("/admin/**")
                        .hasAnyAuthority(
                                "ROLE_attendance_client_admin",
                                "ROLE_attendance_client_superadmin"
                        )

                        // our admin REST API
                        .requestMatchers("/api/admin/**")
                        .hasAnyAuthority(
                                "ROLE_attendance_client_admin",
                                "ROLE_attendance_client_superadmin"
                        )

                        // attendance & excuse APIs require any authenticated user
                        .requestMatchers("/api/attendance/**", "/api/excuse/**")
                        .authenticated()

                        // everything else also needs authentication
                        .anyRequest()
                        .authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/", true)
                        .userInfoEndpoint(userInfo ->
                                userInfo.oidcUserService(oidcUserServiceWithTokenVerifier())
                        )
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(keycloakLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserServiceWithTokenVerifier() {
        OidcUserService delegate = new OidcUserService();
        return userRequest -> {
            String tokenString = userRequest.getAccessToken().getTokenValue();
            Set<GrantedAuthority> mapped = new HashSet<>();

            try {
                AccessToken kcToken = TokenVerifier
                        .create(tokenString, AccessToken.class)
                        .getToken();

                // realm-level roles
                if (kcToken.getRealmAccess() != null) {
                    kcToken.getRealmAccess().getRoles().forEach(r ->
                            mapped.add(new SimpleGrantedAuthority("ROLE_realm_" + r))
                    );
                }

                // client-specific roles
                Map<String, AccessToken.Access> resources = kcToken.getResourceAccess();
                if (resources != null && resources.containsKey("attendance-client")) {
                    resources.get("attendance-client").getRoles().forEach(r ->
                            mapped.add(new SimpleGrantedAuthority(
                                    "ROLE_attendance_client_" + r.replace('-', '_')
                            ))
                    );
                }
            } catch (VerificationException ignored) { }

            OidcUser userInfo = delegate.loadUser(userRequest);
            return new DefaultOidcUser(
                    mapped,
                    userInfo.getIdToken(),
                    userInfo.getUserInfo()
            );
        };
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
}
