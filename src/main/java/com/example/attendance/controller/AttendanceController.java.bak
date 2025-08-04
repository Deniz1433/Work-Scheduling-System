package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceRequest;
import com.example.attendance.dto.ExcuseDto;
import com.example.attendance.dto.ExcusesRequest;
import com.example.attendance.dto.TeamAttendanceDto;
import com.example.attendance.model.Excuse;
import com.example.attendance.service.AttendanceService;

import jakarta.persistence.PrePersist;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @PostMapping("/{id}")
    public ResponseEntity<?> submit(@PathVariable Long id, @RequestBody AttendanceRequest req, Principal principal) {
        System.out.println("Controller received request - ID: " + id + ", Principal: " + principal.getName());
        System.out.println("Request body - weekStart: " + req.getWeekStart() + ", dates: " + req.getDates());
        
        try {
            // Frontend'den gelen userId'yi kullan, principal.getName() değil
            service.record(req.getUserId(), req.getWeekStart(), req.getDates());
            System.out.println("Service call completed successfully");
            return ResponseEntity.ok(Map.of("message", "Attendance updated successfully", "userId", id));
        } catch (Exception e) {
            System.err.println("Error in controller: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error processing request: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, Principal principal) {
        System.out.println("✅ Attendance approval request - ID: " + id + " by user: " + principal.getName());
        service.approve(id, principal.getName());
        System.out.println("✅ Attendance approved successfully!");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/excuse/{id}/approve")
    public ResponseEntity<?> approveExcuse(@PathVariable Long id, Principal principal) {
        service.approveExcuse(id, principal.getName());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<ArrayList<Object>> getAttendanceData(Principal principal, String weekStart) {
        ArrayList<Object> attendanceResponse = service.fetch(principal.getName(), weekStart);
        return ResponseEntity.ok(attendanceResponse);
    }

    @GetMapping("/team")
    public ResponseEntity<List<TeamAttendanceDto>> getTeamAttendance(
            Principal principal,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String roleId,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String attendanceStatus
    ) {
        List<TeamAttendanceDto> team = service.getTeamAttendanceWithFilters(
            principal.getName(), 
            departmentId, 
            roleId, 
            searchTerm, 
            attendanceStatus
        );
        return ResponseEntity.ok(team);
    }

    @GetMapping("/excuse/{id}")
    public ResponseEntity<List<ExcuseDto>> getExcuse(Principal principal, @PathVariable Long id) {
        // Convert the numeric ID to string userId
        String userId = id.toString();
        List<Excuse> excuses = service.getExcuse(principal.getName(), userId);
        List<ExcuseDto> excuseDtos = excuses.stream()
            .map(e -> new ExcuseDto(e.getId(), e.getUserId(), e.getExcuseDate().toString(), e.getExcuseType(), e.getDescription(), e.getIsApproved()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(excuseDtos);
    }
}
