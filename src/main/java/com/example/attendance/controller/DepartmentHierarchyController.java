package com.example.attendance.controller;

import com.example.attendance.model.Department;
import com.example.attendance.model.DepartmentHierarchy;
import com.example.attendance.model.DepartmentNodePosition;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.service.DepartmentHierarchyService;
import com.example.attendance.service.DepartmentNodePositionService;
import com.example.attendance.dto.DepartmentRelationDto;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/admin/hierarchy")
public class DepartmentHierarchyController {
    private final DepartmentHierarchyService hierarchySvc;
    private final DepartmentNodePositionService posSvc;
    private final DepartmentRepository departmentRepo;

    public DepartmentHierarchyController(DepartmentHierarchyService h, DepartmentNodePositionService p, DepartmentRepository d) {
        this.hierarchySvc = h;
        this.posSvc = p;
        this.departmentRepo = d;
    }

    @PostMapping(path="/save", consumes="application/json")
    @ResponseBody
    public ResponseEntity<String> saveAll(@RequestBody SaveDto dto) {
        try {
            // Convert DepartmentRelationDto to DepartmentHierarchy
            List<DepartmentHierarchy> relations = dto.getRelations().stream()
                    .map(rel -> {
                        Department parent = departmentRepo.findByName(rel.getParent())
                                .orElseThrow(() -> new RuntimeException("Parent department not found: " + rel.getParent()));
                        Department child = departmentRepo.findByName(rel.getChild())
                                .orElseThrow(() -> new RuntimeException("Child department not found: " + rel.getChild()));
                        return new DepartmentHierarchy(parent, child);
                    })
                    .toList();
            hierarchySvc.saveRelations(relations);
            
            // build entity list from dto.positions
            List<DepartmentNodePosition> posList = dto.getPositions().stream()
                    .map(p -> new DepartmentNodePosition(p.getDepartment(), p.getX(), p.getY()))
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
                .map(r -> Map.of("parent", r.getParentDepartment().getName(), "child", r.getChildDepartment().getName()))
                .toList();

        Map<String, DepartmentNodePosition> positions = posSvc.loadAll();

        return Map.of(
                "relations", rels,
                "positions", positions
        );
    }

    @Data
    public static class SaveDto {
        private List<DepartmentRelationDto> relations;
        private List<PositionDto> positions;
    }

    @Data
    public static class PositionDto {
        private String department;
        private double x;
        private double y;
    }
}