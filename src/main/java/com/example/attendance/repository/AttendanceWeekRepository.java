package com.example.attendance.repository;

import com.example.attendance.model.AttendanceWeek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceWeekRepository extends JpaRepository<AttendanceWeek, UUID> {
    Optional<AttendanceWeek> findByStartDate(LocalDate startDate);
}
