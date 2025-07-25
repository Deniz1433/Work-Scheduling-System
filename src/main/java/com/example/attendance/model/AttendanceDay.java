package com.example.attendance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "attendance_days")
public class AttendanceDay {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_entry_id", nullable = false)
    private AttendanceEntry attendanceEntry;

    @Column(name = "day_of_week", nullable = false)
    private String dayOfWeek; // e.g., "MONDAY", "TUESDAY", ...

    @Column(name = "status", nullable = false)
    private String status; // "office", "online", "leave", "excuse", "holiday"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "excuse_id")
    private Excuse excuse;
}
