package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceRequest;
import com.example.attendance.dto.ExcuseDto;
import com.example.attendance.dto.TeamAttendanceDto;
import com.example.attendance.model.Excuse;
import com.example.attendance.model.User;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.AttendanceService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    private final AttendanceService service;
    private final UserRepository userRepository;

    public AttendanceController(AttendanceService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        String keycloakId = principal.getName();
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found for keycloak ID: " + keycloakId));
        return user.getId();
    }

    @PostMapping
    public ResponseEntity<?> submit(
            @RequestBody AttendanceRequest req,
            Principal principal
    ) {
        Long userId = getUserIdFromPrincipal(principal);
        service.record(userId, LocalDate.parse(req.getWeekStart()), req.getDates());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}")
    public ResponseEntity<?> submit(@PathVariable Long id, @RequestBody AttendanceRequest req, Principal principal) {
        System.out.println("Controller received request - ID: " + id + ", Principal: " + principal.getName());
        System.out.println("Request body - weekStart: " + req.getWeekStart() + ", dates: " + req.getDates());
        
        try {
            // Frontend'den gelen userId'yi kullan, principal.getName() değil
            service.record(Long.parseLong(req.getUserId()), LocalDate.parse(req.getWeekStart()), req.getDates());
            System.out.println("Service call completed successfully");
            return ResponseEntity.ok(Map.of("message", "Attendance updated successfully", "userId", id));
        } catch (Exception e) {
            System.err.println("Error in controller: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error processing request: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/{weekStart}/approve")
    public ResponseEntity<?> approve(@PathVariable Long userId,@PathVariable String weekStart, Principal principal) {
        System.out.println("✅ Attendance approval request by user: " + principal.getName());
        service.approve(userId, LocalDate.parse(weekStart));
        System.out.println("✅ Attendance approved successfully!");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/excuse/{id}/approve")
    public ResponseEntity<?> approveExcuse(@PathVariable Long id, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        service.approveExcuse(id, userId.toString());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{weekStart}")
    public ResponseEntity<ArrayList<Object>> getAttendanceData(Principal principal, @PathVariable String weekStart) {
        Long userId = getUserIdFromPrincipal(principal);
        ArrayList<Object> attendanceResponse = service.fetch(userId, LocalDate.parse(weekStart));
        return ResponseEntity.ok(attendanceResponse);
    }

    @GetMapping("/team")
    public ResponseEntity<List<TeamAttendanceDto>> getTeamAttendance(
            Principal principal,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String roleId,
            @RequestParam(required = false) String searchTerm
    ) {
        String keycloakId = principal.getName();
        List<TeamAttendanceDto> team = service.getTeamAttendanceWithFilters(
            keycloakId,
            departmentId, 
            roleId, 
            searchTerm
        );
        return ResponseEntity.ok(team);
    }

    @GetMapping("/excuse/{id}")
    public ResponseEntity<List<ExcuseDto>> getExcuse(Principal principal, @PathVariable Long id) {
        Long userId = getUserIdFromPrincipal(principal);
        List<Excuse> excuses = service.getExcuse(userId, id);
        List<ExcuseDto> excuseDtos = excuses.stream()
            .map(e -> new ExcuseDto(e.getId(), e.getUserId(), e.getExcuseDate().toString(), e.getExcuseType(), e.getDescription(), e.getIsApproved()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(excuseDtos);
    }
}