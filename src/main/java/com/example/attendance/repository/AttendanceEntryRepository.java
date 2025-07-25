package com.example.attendance.repository;

import com.example.attendance.model.AttendanceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttendanceEntryRepository extends JpaRepository<AttendanceEntry, UUID> {
    Optional<AttendanceEntry> findByUserIdAndWeekId(UUID userId, UUID weekId);
}
