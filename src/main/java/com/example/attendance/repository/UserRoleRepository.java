package com.example.attendance.repository;

import com.example.attendance.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    
    List<UserRole> findByUserId(String userId);
    
    Optional<UserRole> findByUserIdAndRoleId(String userId, Long roleId);
    
    void deleteByUserIdAndRoleId(String userId, Long roleId);
    
    boolean existsByUserIdAndRoleId(String userId, Long roleId);
} 