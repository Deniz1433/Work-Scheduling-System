// src/main/java/com/example/attendance/RoleHierarchyController.java
package com.example.attendance;

import com.example.attendance.dto.RoleHierarchyDto;
import com.example.attendance.dto.RoleNodePositionDto;
import com.example.attendance.model.RoleHierarchy;
import com.example.attendance.model.RoleNodePosition;
import com.example.attendance.service.AdminService;
import com.example.attendance.service.RoleHierarchyService;
import com.example.attendance.service.RoleNodePositionService;
import com.example.attendance.service.RoleHierarchyService.RoleRelationDto;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import static com.example.attendance.AppConstants.BASE;

/**
 * RoleHierarchyController, rol hiyerarşisi yönetimi için endpoint'ler sağlar.
 */
@Controller
@RequestMapping("/admin/hierarchy")
@PreAuthorize("hasRole('attendance_client_superadmin')")
public class RoleHierarchyController {
    private final RoleHierarchyService hierarchySvc;
    private final RoleNodePositionService posSvc;
    private final AdminService adminSvc;

    public RoleHierarchyController(RoleHierarchyService h, RoleNodePositionService p, AdminService a) {
        this.hierarchySvc = h;
        this.posSvc       = p;
        this.adminSvc     = a;
    }

    /**
     * Rol hiyerarşisi sayfasını görüntüler.
     */
    @GetMapping
    public String viewHierarchy(Model model) {
        // roles
        var allRoles = adminSvc.listClientRoles("attendance-client");
        List<String> roles = hierarchySvc.getFilteredRoles(allRoles, BASE);

        // relations (DTO)
        List<RoleHierarchyDto> links = hierarchySvc.getAllRoleHierarchyDtos();
        Map<String, List<String>> childrenMap = hierarchySvc.buildChildrenMap(hierarchySvc.listAll());

        // roots
        List<String> roots = hierarchySvc.getRootRoles(roles, hierarchySvc.listAll());

        // positions (DTO)
        List<RoleNodePositionDto> posList = posSvc.getAllRoleNodePositionDtos();

        model.addAttribute("roles", roles);
        model.addAttribute("roots", roots);
        model.addAttribute("childrenMap", childrenMap);
        model.addAttribute("positions", posList);

        return "role-hierarchy";
    }

    /**
     * Tüm rol ilişkilerini ve pozisyonlarını kaydeder.
     */
    @PostMapping(path="/save", consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> saveAll(@RequestBody SaveDto dto) {
        try {
            hierarchySvc.saveRelations(dto.getRelations());
            // build entity list from dto.positions
            List<RoleNodePosition> posList = dto.getPositions().stream()
                    .map(p -> new RoleNodePosition(p.getRole(), p.getX(), p.getY()))
                    .toList();
            posSvc.saveAll(posList);
            return ResponseEntity.ok("Saved relations & positions");
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(ex.getMessage());
        }
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
