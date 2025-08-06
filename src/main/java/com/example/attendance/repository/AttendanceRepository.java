package com.example.attendance.repository;

import com.example.attendance.model.Attendance;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Attendance findByUserIdAndWeekStart(Long userId, LocalDate weekStart);
    List<Attendance> findByUserId(Long userId);
   //UserId + weekStart ile attendance silme
    void deleteByUserIdAndWeekStart(
            Long userId,
            LocalDate weekStart
    );
}
