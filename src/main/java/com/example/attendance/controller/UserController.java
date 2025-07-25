// src/main/java/com/example/attendance/controller/UserController.java
package com.example.attendance.controller;

import com.example.attendance.dto.CreateUserDto;
import com.example.attendance.dto.UserDto;
import com.example.attendance.service.UserService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Returns details about the currently authenticated user
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(@AuthenticationPrincipal OidcUser user) {
        Map<String, Object> response = new HashMap<>();
        response.put("username", user.getPreferredUsername());
        response.put("email", user.getEmail());
        response.put("authorities", user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        return response;
    }

    // Search users by name, surname, or email
    @GetMapping("/users")
    public List<UserDto> searchUsers(
            @RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String surname,
            @RequestParam(defaultValue = "") String email
    ) {
        return userService.search(name, surname, email);
    }

    // Create a new user (provisions in Keycloak and mirrors locally)
    @PostMapping("/users")
    public UserDto createUser(@Valid @RequestBody CreateUserDto dto) {
        return userService.create(dto);
    }

    // Delete a user by UUID (also removes from Keycloak)
    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable UUID id) {
        userService.delete(id);
    }
}
