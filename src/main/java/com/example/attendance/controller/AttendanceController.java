// src/main/java/com/example/attendance/controller/AttendanceController.java
package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceSubmissionDto;
import com.example.attendance.service.AttendanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    public List<Integer> getAttendanceWeek(
            @RequestParam String weekStart,
            @AuthenticationPrincipal OidcUser user
    ) {
        return attendanceService.getWeekAttendance(
                user.getSubject(), // keycloak_id
                LocalDate.parse(weekStart)
        );
    }

    @PostMapping
    public void submitAttendance(
            @Valid @RequestBody AttendanceSubmissionDto dto,
            @AuthenticationPrincipal OidcUser user
    ) {
        attendanceService.saveOrUpdate(
                user.getSubject(),
                LocalDate.parse(dto.getWeekStart()),
                dto.getDates()
        );
    }
}
