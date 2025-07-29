// src/main/java/com/example/attendance/AdminController.java
package com.example.attendance;

import com.example.attendance.dto.AdminUserDto;
import com.example.attendance.dto.CreateRoleDto;
import com.example.attendance.service.AdminService;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.BindingResult;
import java.util.stream.Collectors;
import static com.example.attendance.AppConstants.CLIENT;
import static com.example.attendance.AppConstants.BASE_ROLES;

/**
 * AdminController, admin işlemleri için endpoint'ler sağlar.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminSvc;

    public AdminController(AdminService adminSvc) {
        this.adminSvc = adminSvc;
    }

    /**
     * Admin dashboard sayfasını görüntüler.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('attendance_client_admin','attendance_client_superadmin')")
    public String dashboard(Model model) {
        List<AdminUserDto> adminUsers = adminSvc.getAllAdminUserDtos(CLIENT, BASE_ROLES);
        List<RoleRepresentation> allRoles = adminSvc.getAllRoles(CLIENT);
        model.addAttribute("adminUsers", adminUsers);
        model.addAttribute("roles", allRoles);
        model.addAttribute("baseRoles", BASE_ROLES);
        return "admin-dashboard";
    }

    /**
     * Kullanıcının rollerini günceller.
     */
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

    /**
     * Yeni bir rol oluşturur.
     */
    @PostMapping("/roles")
    @PreAuthorize("hasRole('attendance_client_superadmin')")
    public String createRole(@Valid @ModelAttribute CreateRoleDto dto, BindingResult result, Model model) {
        if (result.hasErrors()) {
            // Hataları modele ekle ve dashboard'a geri dön
            model.addAttribute("roleError", result.getFieldError("roleName").getDefaultMessage());
            return dashboard(model);
        }
        adminSvc.createClientRole(CLIENT, dto.getRoleName());
        return "redirect:/admin/dashboard";
    }

    /**
     * Bir rolü siler.
     */
    @PostMapping("/roles/{roleName}/delete")
    @PreAuthorize("hasRole('attendance_client_superadmin')")
    public String deleteRole(@PathVariable String roleName) {
        if (!BASE_ROLES.contains(roleName)) {
            adminSvc.deleteClientRole(CLIENT, roleName);
        }
        return "redirect:/admin/dashboard";
    }
}
