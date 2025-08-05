package com.example.attendance.repository;

import com.example.attendance.model.Attendance;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Attendance findByUserIdAndWeekStart(Long userId, LocalDate weekStart);

   //UserId + weekStart ile attendance silme
    void deleteByUserIdAndWeekStart(
            Long userId,
            LocalDate weekStart
    );
}
