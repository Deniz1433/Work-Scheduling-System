// src/main/java/com/example/attendance/AdminController.java
package com.example.attendance;

import com.example.attendance.dto.AdminUserDto;
import com.example.attendance.service.AdminService;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminSvc;
    private static final String CLIENT = "attendance-client";
    private static final List<String> BASE_ROLES = List.of("admin","superadmin","user");

    public AdminController(AdminService adminSvc) {
        this.adminSvc = adminSvc;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('attendance_client_admin','attendance_client_superadmin')")
    public String dashboard(Model model) {
        List<AdminUserDto> adminUsers = adminSvc.listAllUsers().stream().map(u -> {
            List<String> roles = adminSvc.getUserClientRoles(u.getId(), CLIENT)
                    .stream()
                    .map(RoleRepresentation::getName)
                    .toList();
            return new AdminUserDto(u.getId(), u.getUsername(), u.getEmail(), roles);
        }).toList();

        List<RoleRepresentation> allRoles = adminSvc.listClientRoles(CLIENT);

        model.addAttribute("adminUsers", adminUsers);
        model.addAttribute("roles", allRoles);
        model.addAttribute("baseRoles", BASE_ROLES);
        return "admin-dashboard";
    }

    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('attendance_client_superadmin')")
    public String updateRoles(
            @PathVariable String userId,
            @RequestParam(required = false) List<String> selectedRoles) {

        if (selectedRoles == null) selectedRoles = List.of();

        var allRoleNames = adminSvc.listClientRoles(CLIENT)
                .stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());

        adminSvc.removeClientRolesFromUser(userId, CLIENT, allRoleNames);
        adminSvc.addClientRolesToUser(userId, CLIENT, selectedRoles);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('attendance_client_superadmin')")
    public String createRole(@RequestParam String roleName) {
        adminSvc.createClientRole(CLIENT, roleName);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/roles/{roleName}/delete")
    @PreAuthorize("hasRole('attendance_client_superadmin')")
    public String deleteRole(@PathVariable String roleName) {
        if (!BASE_ROLES.contains(roleName)) {
            adminSvc.deleteClientRole(CLIENT, roleName);
        }
        return "redirect:/admin/dashboard";
    }
}
