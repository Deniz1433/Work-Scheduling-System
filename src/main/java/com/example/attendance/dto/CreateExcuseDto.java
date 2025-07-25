// src/main/java/com/example/attendance/dto/CreateExcuseDto.java
package com.example.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateExcuseDto {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private String reason;

    private String description;
}
