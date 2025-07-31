package com.example.attendance.repository;

import com.example.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    // Departman ID'sine göre kullanıcıları getir
    List<User> findByDepartmentId(Long departmentId);
    
    // Username'e göre kullanıcı getir
    User findByUsername(String username);
}
