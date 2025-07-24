package com.example.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.attendance.model.Department;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
