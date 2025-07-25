// src/main/java/com/example/attendance/dto/ExcuseDto.java
package com.example.attendance.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class ExcuseDto {

    private UUID id;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String description;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime approvedAt;
    private String approvedBy;
}