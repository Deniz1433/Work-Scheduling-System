package com.example.attendance.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.attendance.service.DepartmentService;
import com.example.attendance.dto.DepartmentDto;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/admin/department")
public class DepartmentController {

      private final DepartmentService service;

      public DepartmentController(DepartmentService service) {
            this.service = service;
      }

      @PostMapping
      public ResponseEntity<DepartmentDto> addDepartment(@RequestBody DepartmentDto departmentDto) {
            DepartmentDto saved = service.addDepartment(departmentDto);
            return ResponseEntity.ok(saved);
      }

      @PostMapping("/{id}")
      public ResponseEntity<DepartmentDto> updateDepartment(
            @PathVariable Long id,
             @RequestBody DepartmentDto departmentDto
      ) {
            DepartmentDto updated = service.updateDepartment(id, departmentDto);
            return ResponseEntity.ok(updated);
      }

      @DeleteMapping("/{id}")
      public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
            service.deleteDepartment(id);
            return ResponseEntity.noContent().build();
      }

      @GetMapping
      public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
            List<DepartmentDto> departments = service.getAllDepartments();
            return ResponseEntity.ok(departments);
      }

      @GetMapping("/{id}")
      public ResponseEntity<DepartmentDto> getDepartmentById(@PathVariable Long id) {
            DepartmentDto department = service.getDepartmentById(id);
            return ResponseEntity.ok(department);
      }

}
