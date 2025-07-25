// src/main/java/com/example/attendance/dto/AttendanceSubmissionDto.java
package com.example.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AttendanceSubmissionDto {
    @NotNull
    private String weekStart;

    @NotNull
    private List<Integer> dates;
}
