package com.example.attendance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    /**
     * GET /api/user
     * Returns basic info about the logged-in user.
     */
    @GetMapping("/user")
    public Map<String, Object> getUser(@AuthenticationPrincipal OidcUser user) {
        if (user == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> info = new HashMap<>();
        info.put("name", user.getName());
        info.put("preferredUsername", user.getPreferredUsername());
        info.put("email", user.getEmail());
        info.put("authorities", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        log.debug("Returning user info: {}", info);
        return info;
    }
}