package com.example.attendance.repository;

import com.example.attendance.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByUserId(String userId);

    // Spring Data will derive this and perform the delete
    void deleteByUserIdAndAttendanceDateBetween(
            String userId,
            LocalDate start,
            LocalDate end
    );
}
