// src/main/java/com/example/attendance/config/SecurityConfig.java
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
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/static/**", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**"
                        ).permitAll()
                        .requestMatchers("/api/attendance/**").authenticated()
                        .anyRequest().authenticated()
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
        RetryTemplate retry = new RetryTemplate();

        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(10);
        FixedBackOffPolicy backoff = new FixedBackOffPolicy();
        backoff.setBackOffPeriod(2000);

        retry.setRetryPolicy(policy);
        retry.setBackOffPolicy(backoff);

        return userRequest -> {
            String tokenValue = userRequest.getAccessToken().getTokenValue();
            Set<GrantedAuthority> mapped = new HashSet<>();

            try {
                AccessToken kcToken = retry.execute(ctx -> {
                    log.debug("Verifying Keycloak token (try #{})", ctx.getRetryCount() + 1);
                    return TokenVerifier.create(tokenValue, AccessToken.class).getToken();
                });

                Map<String, AccessToken.Access> resources = kcToken.getResourceAccess();
                if (resources != null && resources.containsKey("attendance-client")) {
                    resources.get("attendance-client")
                            .getRoles()
                            .forEach(r -> mapped.add(
                                    new SimpleGrantedAuthority("ROLE_attendance_client_" + r.replace('-', '_'))
                            ));
                }
            } catch (VerificationException e) {
                log.warn("Keycloak token verification failed: {}", e.getMessage());
            }

            OidcUser user = delegate.loadUser(userRequest);
            log.debug("Logged in user: {}", user.getEmail());
            return new DefaultOidcUser(mapped, user.getIdToken(), user.getUserInfo());
        };
    }

    private LogoutSuccessHandler keycloakLogoutSuccessHandler() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                Authentication auth) -> {

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
