package com.example.attendance.controller;

import com.example.attendance.service.KeycloakAdminService;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminManageRolesController {

    private static final Set<String> BASE_ROLES = Set.of("user", "admin", "superadmin");

    @Autowired
    private KeycloakAdminService kcService;

    public static class UserRolesDto {
        public String id;
        public String username;
        public String email;
        public String permissionRole;
        public List<String> departmentRoles;
    }

    @GetMapping("/users")
    public List<UserRolesDto> listAllUsers() {
        return kcService.getAllUsers().stream().map(u -> {
            List<String> roles = kcService.getUserClientRoles(u.getId());
            String perm = roles.stream()
                    .filter(BASE_ROLES::contains)
                    .findFirst()
                    .orElse("user");
            List<String> deps = roles.stream()
                    .filter(r -> !BASE_ROLES.contains(r))
                    .collect(Collectors.toList());
            UserRolesDto dto = new UserRolesDto();
            dto.id = u.getId();
            dto.username = u.getUsername();
            dto.email = u.getEmail();
            dto.permissionRole = perm;
            dto.departmentRoles = deps;
            return dto;
        }).collect(Collectors.toList());
    }

    @GetMapping("/roles/permissions")
    public List<String> listPermissionRoles() {
        return kcService.getPermissionRoles().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    @GetMapping("/roles/departments")
    public List<String> listDepartmentRoles() {
        return kcService.getDepartmentRoles().stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());
    }

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<Void> updateUserRoles(
            @PathVariable String userId,
            @RequestBody Map<String, Object> payload
    ) {
        String newPerm = (String) payload.get("permissionRole");
        @SuppressWarnings("unchecked")
        List<String> newDeps = (List<String>) payload.getOrDefault("departmentRoles", List.of());

        // Determine current caller's permission
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperadmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_attendance_client_superadmin"));
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_attendance_client_admin"));

        // Fetch target user's current permission
        List<String> currentRoles = kcService.getUserClientRoles(userId);
        String currentPerm = currentRoles.stream()
                .filter(BASE_ROLES::contains)
                .findFirst()
                .orElse("user");

        // Superadmin may not assign or modify other superadmins
        if (isSuperadmin) {
            if ("superadmin".equals(currentPerm) || "superadmin".equals(newPerm)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }
        // Admin may only manage users: cannot touch admins or superadmins
        else if (isAdmin) {
            if (!"user".equals(currentPerm) || !"user".equals(newPerm)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            // neither admin nor superadmin
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // All checks passed: combine and update
        List<String> allRoles = new ArrayList<>();
        allRoles.add(newPerm);
        allRoles.addAll(newDeps);
        kcService.updateUserClientRoles(userId, allRoles);

        return ResponseEntity.noContent().build();
    }
}
