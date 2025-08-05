// src/main/java/com/example/attendance/RoleHierarchyController.java
package com.example.attendance;

import com.example.attendance.model.DepartmentHierarchy;
import com.example.attendance.model.DepartmentNodePosition;
import com.example.attendance.service.KeycloakAdminService;
import com.example.attendance.service.DepartmentHierarchyService;
import com.example.attendance.service.DepartmentNodePositionService;
import com.example.attendance.service.DepartmentHierarchyService.RoleRelationDto;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/hierarchy")
@PreAuthorize("hasRole('attendance_client_superadmin')")
public class DepartmentHierarchyController {
    private final DepartmentHierarchyService hierarchySvc;
    private final DepartmentNodePositionService posSvc;
    private final KeycloakAdminService adminSvc;
    private static final Set<String> BASE = Set.of("admin","user","superadmin");

    public DepartmentHierarchyController(DepartmentHierarchyService h, DepartmentNodePositionService p, KeycloakAdminService a) {
        this.hierarchySvc = h;
        this.posSvc       = p;
        this.adminSvc     = a;
    }

    @GetMapping
    public String viewHierarchy(Model model) {
        // roles
        List<String> roles = adminSvc.listClientRoles("attendance-client").stream()
                .map(RoleRepresentation::getName)
                .filter(r -> !BASE.contains(r))
                .collect(Collectors.toList());

        // relations
        List<DepartmentHierarchy> links = hierarchySvc.listAll();
        Map<String,List<String>> childrenMap = new HashMap<>();
        for (var l : links) {
            childrenMap.computeIfAbsent(l.getParentRole(), k->new ArrayList<>())
                    .add(l.getChildRole());
        }

        // roots
        Set<String> allChildren = links.stream()
                .map(DepartmentHierarchy::getChildRole)
                .collect(Collectors.toSet());
        List<String> roots = roles.stream()
                .filter(r -> !allChildren.contains(r))
                .collect(Collectors.toList());

        // positions
        Map<String, DepartmentNodePosition> posMap = posSvc.loadAll();

        model.addAttribute("roles", roles);
        model.addAttribute("roots", roots);
        model.addAttribute("childrenMap", childrenMap);
        model.addAttribute("positionsMap", posMap);

        return "role-hierarchy";
    }



    @PostMapping(path="/save", consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> saveAll(@RequestBody SaveDto dto) {
        try {
            hierarchySvc.saveRelations(dto.getRelations());
            // build entity list from dto.positions
            List<DepartmentNodePosition> posList = dto.getPositions().stream()
                    .map(p -> new DepartmentNodePosition(p.getRole(), p.getX(), p.getY()))
                    .toList();
            posSvc.saveAll(posList);
            return ResponseEntity.ok("Saved relations & positions");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(ex.getMessage());
        }
    }

    @GetMapping("/load")
    @ResponseBody
    public Map<String, Object> loadHierarchy() {
        List<DepartmentHierarchy> links = hierarchySvc.listAll();
        List<Map<String, String>> rels = links.stream()
                .map(r -> Map.of("parent", r.getParentRole(), "child", r.getChildRole()))
                .toList();

        Map<String, DepartmentNodePosition> positions = posSvc.loadAll();

        return Map.of(
                "relations", rels,
                "positions", positions
        );
    }


    public static class SaveDto {
        private List<RoleRelationDto> relations;
        private List<PositionDto> positions;
        public List<RoleRelationDto> getRelations() { return relations; }
        public void setRelations(List<RoleRelationDto> r) { this.relations = r; }
        public List<PositionDto> getPositions() { return positions; }
        public void setPositions(List<PositionDto> p) { this.positions = p; }
    }

    public static class PositionDto {
        private String role;
        private double x, y;
        public String getRole() { return role; }
        public void setRole(String r) { this.role = r; }
        public double getX() { return x; }
        public void setX(double v) { this.x = v; }
        public double getY() { return y; }
        public void setY(double v) { this.y = v; }
    }
}
