package com.example.attendance.repository;

import com.example.attendance.model.Excuse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExcuseRepository extends JpaRepository<Excuse, Long> {
    List<Excuse> findByUserId(Long userId);
}
