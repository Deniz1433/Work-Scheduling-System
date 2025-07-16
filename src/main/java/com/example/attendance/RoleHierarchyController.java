// src/main/java/com/example/attendance/RoleHierarchyController.java
package com.example.attendance;

import com.example.attendance.model.RoleHierarchy;
import com.example.attendance.service.AdminService;
import com.example.attendance.service.RoleHierarchyService;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/hierarchy")
@PreAuthorize("hasRole('attendance_client_superadmin')")
public class RoleHierarchyController {

    private final RoleHierarchyService hierarchySvc;
    private final AdminService adminSvc;
    private static final Set<String> BASE = Set.of("admin","user","superadmin");

    public RoleHierarchyController(RoleHierarchyService hierarchySvc,
                                   AdminService adminSvc) {
        this.hierarchySvc = hierarchySvc;
        this.adminSvc      = adminSvc;
    }

    @GetMapping
    public String viewHierarchy(Model model) {
        // all non-base client roles
        List<String> roles = adminSvc.listClientRoles("attendance-client")
                .stream()
                .map(RoleRepresentation::getName)
                .filter(r -> !BASE.contains(r))
                .collect(Collectors.toList());

        // all parent→child links
        List<RoleHierarchy> links = hierarchySvc.listAll();

        // build children map
        Map<String,List<String>> childrenMap = new HashMap<>();
        for (RoleHierarchy l : links) {
            if (!childrenMap.containsKey(l.getParentRole())) {
                childrenMap.put(l.getParentRole(), new ArrayList<>());
            }
            childrenMap.get(l.getParentRole()).add(l.getChildRole());
        }

        // find roots: roles that never appear as a child
        Set<String> allChildren = links.stream()
                .map(RoleHierarchy::getChildRole)
                .collect(Collectors.toSet());
        List<String> roots = roles.stream()
                .filter(r -> !allChildren.contains(r))
                .collect(Collectors.toList());

        model.addAttribute("roles", roles);
        model.addAttribute("roots", roots);
        model.addAttribute("childrenMap", childrenMap);
        return "role-hierarchy";
    }

    @PostMapping("/add")
    public String addLink(@RequestParam String parent,
                          @RequestParam String child) {
        hierarchySvc.addRelation(parent, child);
        return "redirect:/admin/hierarchy";
    }

    @PostMapping("/remove")
    public String removeLink(@RequestParam String parent,
                             @RequestParam String child) {
        hierarchySvc.removeRelation(parent, child);
        return "redirect:/admin/hierarchy";
    }
}
