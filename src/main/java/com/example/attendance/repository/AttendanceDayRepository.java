package com.example.attendance.repository;

import com.example.attendance.model.AttendanceDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttendanceDayRepository extends JpaRepository<AttendanceDay, UUID> {
    List<AttendanceDay> findByAttendanceEntryId(UUID attendanceEntryId);
    void deleteByAttendanceEntryId(UUID attendanceEntryId);
}
