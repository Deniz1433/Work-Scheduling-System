package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceRequest;
import com.example.attendance.dto.TeamAttendanceDto;
import com.example.attendance.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<?> submit(
            @RequestBody AttendanceRequest req,
            Principal principal
    ) {
        service.record(principal.getName(),req.getWeekStart(),req.getDates());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<Integer>> getAttendanceData(Principal principal, String weekStart) {
        List<Integer> dates = service.fetch(principal.getName(), weekStart);
        return ResponseEntity.ok(dates);
    }

    @GetMapping("/team")
    public ResponseEntity<List<TeamAttendanceDto>> getTeamAttendance(Principal principal) {
        List<TeamAttendanceDto> team = service.getTeamAttendance(principal.getName());
        return ResponseEntity.ok(team);
    }
}
