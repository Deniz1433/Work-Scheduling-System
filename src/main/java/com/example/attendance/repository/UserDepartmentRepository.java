package com.example.attendance.repository;

import com.example.attendance.model.UserDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDepartmentRepository extends JpaRepository<UserDepartment, Long> {
    
    List<UserDepartment> findByUserId(String userId);
    
    Optional<UserDepartment> findByUserIdAndDepartmentId(String userId, Long departmentId);
    
    void deleteByUserIdAndDepartmentId(String userId, Long departmentId);
    
    void deleteByUserId(String userId);
    
    boolean existsByUserIdAndDepartmentId(String userId, Long departmentId);
} 