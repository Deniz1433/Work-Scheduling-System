package com.example.attendance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "attendance_entries")
public class AttendanceEntry {

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "week_id", nullable = false)
    private AttendanceWeek week;

    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    private String status; // optional, e.g. "pending", "approved"
    private String comment;
}
