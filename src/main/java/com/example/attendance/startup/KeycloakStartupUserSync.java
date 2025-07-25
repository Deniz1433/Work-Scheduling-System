// src/main/java/com/example/attendance/startup/KeycloakStartupUserSync.java
package com.example.attendance.startup;

import com.example.attendance.model.User;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakStartupUserSync implements ApplicationListener<ApplicationReadyEvent> {

    private final Keycloak keycloak;
    private final UserRepository userRepository;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(10); // Try up to 10 times

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000); // 3 seconds between retries

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        retryTemplate.execute(context -> {
            log.info("🔄 Attempting to sync Keycloak users (attempt #{})", context.getRetryCount() + 1);

            List<UserRepresentation> keycloakUsers = keycloak
                    .realm("attendance-realm")
                    .users()
                    .list();

            for (UserRepresentation rep : keycloakUsers) {
                if (userRepository.findByKeycloakId(rep.getId()).isEmpty()) {
                    User user = new User();
                    user.setId(UUID.randomUUID());
                    user.setKeycloakId(rep.getId());
                    user.setUsername(rep.getUsername());
                    user.setEmail(rep.getEmail());
                    user.setName(rep.getFirstName());
                    user.setSurname(rep.getLastName());
                    user.setIsActive(true);
                    userRepository.save(user);
                    log.info("✅ Synced user: {}", user.getUsername());
                }
            }

            log.info("✅ Finished syncing Keycloak users.");
            return null;
        });
    }
}
