package com.example.attendance.controller;

import com.example.attendance.model.Department;
import com.example.attendance.model.User;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.DepartmentService;
import com.example.attendance.service.DepartmentHierarchyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {
    private final DepartmentService departmentService;
    private final DepartmentHierarchyService departmentHierarchyService;
    private final UserRepository userRepository;

    public DepartmentController(DepartmentService departmentService, 
                              DepartmentHierarchyService departmentHierarchyService,
                              UserRepository userRepository) {
        this.departmentService = departmentService;
        this.departmentHierarchyService = departmentHierarchyService;
        this.userRepository = userRepository;
    }


    @GetMapping
    public ResponseEntity<List<Department>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/child-departments")
    public ResponseEntity<List<Department>> getChildDepartments(Principal principal) {
        try {
            String keycloakId = principal.getName();
            User user = userRepository.findByKeycloakId(keycloakId).orElse(null);
            
            if (user == null || user.getDepartment() == null) {
                return ResponseEntity.ok(List.of());
            }
            
            Set<Department> childDepartments = departmentHierarchyService.findAllDescendants(user.getDepartment());
            List<Department> result = childDepartments.stream().toList();
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("Error getting child departments: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'CREATE_DEPARTMENT'})")
    @PostMapping
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        try {
            Department createdDepartment = departmentService.createDepartment(department);
            return ResponseEntity.ok(createdDepartment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_DEPARTMENT'})")
    @PutMapping("/{id}")
    public ResponseEntity<Department> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        try {
            if(department.getMinDays() < 1) {
                department.setMinDays(1);
            }
            if(department.getMinDays() > 5) {
                department.setMinDays(5);
            }   
            Department updatedDepartment = departmentService.updateDepartment(id, department);
            return ResponseEntity.ok(updatedDepartment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_DEPARTMENT'})")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        try {
            departmentService.deleteDepartment(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 