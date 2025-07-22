package com.example.attendance.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "excuse")
public class Excuse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="user_id", nullable=false, length=36)
    private String userId;

    @Column(name="excuse_date", nullable=false)
    private LocalDate excuseDate;

    @Column(name="excuse_type", nullable=false, length=50)
    private String excuseType;

    @Column(name="description", columnDefinition="TEXT")
    private String description;

    @Column(name="created_at", nullable=false, updatable=false)
    private Instant createdAt = Instant.now();

    @Column(name="is_approved", nullable=false)
    private Boolean isApproved = false;

    public Excuse() {}

    public Excuse(String userId, LocalDate excuseDate, String excuseType, String description) {
        this.userId = userId;
        this.excuseDate = excuseDate;
        this.excuseType = excuseType;
        this.description = description;
    }

    // --- getters & setters ---
    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDate getExcuseDate() { return excuseDate; }
    public void setExcuseDate(LocalDate excuseDate) { this.excuseDate = excuseDate; }
    public String getExcuseType() { return excuseType; }
    public void setExcuseType(String excuseType) { this.excuseType = excuseType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean isApproved) { this.isApproved = isApproved; }
}
