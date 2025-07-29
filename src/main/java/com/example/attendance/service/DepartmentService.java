package com.example.attendance.service;

import org.springframework.stereotype.Service;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.dto.DepartmentDto;
import com.example.attendance.model.Department;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {
      private final DepartmentRepository repo;

      public DepartmentService(DepartmentRepository repo) {
            this.repo = repo;
      }

      public List<DepartmentDto> getAllDepartments() {
            return repo.findAll().stream()
                        .map(d -> new DepartmentDto(d.getId(), d.getName(), d.getMinDays(), 
                             d.getChildDepartment() != null ? d.getChildDepartment().getId() : null))
                        .collect(Collectors.toList());
      }

      public DepartmentDto getDepartmentById(Long id) {
            Department department = repo.findById(id)
                        .orElseThrow(() -> new RuntimeException("Department not found"));
            return new DepartmentDto(department.getId(), department.getName(), department.getMinDays(), 
                                   department.getChildDepartment() != null ? department.getChildDepartment().getId() : null);
      }

      public DepartmentDto addDepartment(DepartmentDto departmentDto) {
            Department department = new Department();
            department.setName(departmentDto.getName());
            department.setMinDays(departmentDto.getMinDays());
            Department saved = repo.save(department);
            return new DepartmentDto(saved.getId(), saved.getName(), saved.getMinDays(), 
                                   saved.getChildDepartment() != null ? saved.getChildDepartment().getId() : null);
      }

      public DepartmentDto updateDepartment(Long id, DepartmentDto departmentDto) {
            Department department = repo.findById(id)
                        .orElseThrow(() -> new RuntimeException("Department not found"));
            department.setName(departmentDto.getName());
            department.setMinDays(departmentDto.getMinDays());
            Department saved = repo.save(department);
            return new DepartmentDto(saved.getId(), saved.getName(), saved.getMinDays(), 
                                   saved.getChildDepartment() != null ? saved.getChildDepartment().getId() : null);
      }

      public void deleteDepartment(Long id) {
            repo.deleteById(id);
      }
}
