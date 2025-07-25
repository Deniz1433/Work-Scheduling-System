package com.example.attendance.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
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
import com.example.attendance.service.KeycloakOidcUserSyncService;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private final KeycloakOidcUserSyncService syncService;

    public SecurityConfig(KeycloakOidcUserSyncService syncService) {
        this.syncService = syncService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/static/**",
                                "/favicon.ico",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()
                        .requestMatchers("/api/attendance/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .defaultSuccessUrl("/", true)
                        .userInfoEndpoint(userInfo ->
                                // here we swap in your sync service so users get upserted on login
                                userInfo.oidcUserService(syncService)
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

        RetryTemplate retryTemplate = new RetryTemplate();
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(10);
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000);
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        return userRequest -> {
            String tokenString = userRequest.getAccessToken().getTokenValue();
            Set<GrantedAuthority> mapped = new HashSet<>();

            try {
                AccessToken kcToken = retryTemplate.execute(context -> {
                    log.debug("Attempting to verify Keycloak token (try #{})", context.getRetryCount() + 1);
                    return TokenVerifier.create(tokenString, AccessToken.class).getToken();
                });

                Map<String, AccessToken.Access> resources = kcToken.getResourceAccess();
                if (resources != null && resources.containsKey("attendance-client")) {
                    resources.get("attendance-client").getRoles().forEach(r ->
                            mapped.add(new SimpleGrantedAuthority("ROLE_attendance_client_" + r.replace('-', '_')))
                    );
                }

            } catch (VerificationException e) {
                log.warn("Keycloak token verification failed after retries: {}", e.getMessage());
            }

            OidcUser userInfo = delegate.loadUser(userRequest);
            log.debug("Logged in user: {}", userInfo.getEmail());
            return new DefaultOidcUser(mapped, userInfo.getIdToken(), userInfo.getUserInfo());
        };
    }

    private LogoutSuccessHandler keycloakLogoutSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication auth) -> {
            String redirectUri = "http://localhost:8080/";
            String idTokenHint = "";

            if (auth != null && auth.getPrincipal() instanceof OidcUser u) {
                idTokenHint = u.getIdToken().getTokenValue();
                log.debug("Logging out user: {}", u.getEmail());
            } else {
                log.warn("No valid OIDC principal found during logout.");
            }

            String logoutUrl = UriComponentsBuilder
                    .fromUriString("http://localhost:8081/realms/attendance-realm/protocol/openid-connect/logout")
                    .queryParam("id_token_hint", idTokenHint)
                    .queryParam("post_logout_redirect_uri", redirectUri)
                    .toUriString();

            log.debug("Redirecting to Keycloak logout URL: {}", logoutUrl);
            response.sendRedirect(logoutUrl);
        };
    }
}
