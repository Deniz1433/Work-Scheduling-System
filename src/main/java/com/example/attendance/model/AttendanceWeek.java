package com.example.attendance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "attendance_weeks")
public class AttendanceWeek {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "start_date", nullable = false, unique = true)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    private Date createdAt;
}
