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

    public AdminController(AdminService adminSvc) {
        this.adminSvc = adminSvc;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('attendance_client_admin') or hasRole('attendance_client_superadmin')")
    public String dashboard(Model model) {
        List<UserRepresentation> users = adminSvc.listAllUsers();
        List<AdminUserDto> adminUsers = users.stream().map(u -> {
            List<String> clientRoles = adminSvc.getUserClientRoles(u.getId(), "attendance-client")
                    .stream()
                    .map(RoleRepresentation::getName)
                    .toList();
            return new AdminUserDto(u.getId(), u.getUsername(), u.getEmail(), clientRoles);
        }).toList();

        List<RoleRepresentation> clientRoles = adminSvc.listClientRoles("attendance-client");
        model.addAttribute("adminUsers", adminUsers);
        model.addAttribute("roles", clientRoles);
        return "admin-dashboard";
    }

    @PostMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('attendance_client_superadmin')")
    public String updateRoles(
            @PathVariable String userId,
            @RequestParam(required = false) List<String> selectedRoles) {

        if (selectedRoles == null) selectedRoles = List.of();

        List<String> allRoles = adminSvc.listClientRoles("attendance-client")
                .stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toList());

        adminSvc.removeClientRolesFromUser(userId, "attendance-client", allRoles);
        adminSvc.addClientRolesToUser(userId, "attendance-client", selectedRoles);

        return "redirect:/admin/dashboard";
    }
}
