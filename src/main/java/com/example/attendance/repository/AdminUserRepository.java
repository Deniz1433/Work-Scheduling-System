package com.example.attendance.repository;

import com.example.attendance.model.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, String> {
    // Gerekirse ek sorgular buraya eklenir
}
