package com.example.attendance.repository;

import com.example.attendance.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Attendance findByUserIdAndWeekStart(String userId, String weekStart);
    
    List<Attendance> findByUserId(String userId);

    // Spring Data will derive this and perform the delete
    void deleteByUserIdAndWeekStart(
            String userId,
            String weekStart
    );
}
