package com.example.attendance.service;

import com.example.attendance.model.Department;
import com.example.attendance.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Transactional
    public Department createDepartment(Department department) {
        if (departmentRepository.existsByName(department.getName())) {
            throw new RuntimeException("Bu departman adı zaten mevcut: " + department.getName());
        }
        return departmentRepository.save(department);
    }

    @Transactional
    public Department updateDepartment(Long id, Department department) {
        Department existingDepartment = departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Departman bulunamadı: " + id));

        if (!existingDepartment.getName().equals(department.getName()) &&
                departmentRepository.existsByName(department.getName())) {
            throw new RuntimeException("Bu departman adı zaten mevcut: " + department.getName());
        }

        existingDepartment.setName(department.getName());
        existingDepartment.setMinDays(department.getMinDays());
        return departmentRepository.save(existingDepartment);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new RuntimeException("Departman bulunamadı: " + id);
        }
        departmentRepository.deleteById(id);
    }
}
