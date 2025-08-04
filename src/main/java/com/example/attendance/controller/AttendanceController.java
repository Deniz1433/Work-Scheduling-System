package com.example.attendance.controller;

import com.example.attendance.dto.TeamAttendanceDto;
import com.example.attendance.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAttendance(@RequestParam(required = false) String weekStart) {
        try {
            // Kullanıcının kendi attendance verilerini getir
            if (weekStart == null) {
                // Gelecek haftanın başlangıç tarihini hesapla
                LocalDate nextWeekStart = LocalDate.now();
                int dayOfWeek = nextWeekStart.getDayOfWeek().getValue();
                int daysUntilNextMonday = (8 - dayOfWeek) % 7;
                nextWeekStart = nextWeekStart.plusDays(daysUntilNextMonday);
                weekStart = nextWeekStart.toString();
            }
            
            // Şimdilik mock data döndür, gerçek uygulamada authentication'dan user ID alınacak
            return ResponseEntity.ok(Map.of(
                "message", "Attendance data retrieved successfully",
                "weekStart", weekStart,
                "data", List.of(List.of(0, 0, 0, 0, 0), false) // [attendance_dates, is_approved]
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get attendance: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/team")
    public ResponseEntity<List<TeamAttendanceDto>> getTeamAttendance(
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String roleId,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String attendanceStatus
    ) {
        try {
            List<TeamAttendanceDto> teamAttendance = attendanceService.getTeamAttendanceWithFilters(
                "admin", // Admin kullanıcısı için - gerçek uygulamada authentication'dan alınacak
                departmentId,
                roleId,
                searchTerm,
                attendanceStatus
            );
            return ResponseEntity.ok(teamAttendance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserAttendance(@PathVariable String userId) {
        try {
            // Kullanıcının gelecek hafta için attendance verilerini getir
            LocalDate nextWeekStart = LocalDate.now();
            int dayOfWeek = nextWeekStart.getDayOfWeek().getValue();
            int daysUntilNextMonday = (8 - dayOfWeek) % 7;
            nextWeekStart = nextWeekStart.plusDays(daysUntilNextMonday);
            
            var attendanceData = attendanceService.fetch(userId, nextWeekStart.toString());
            
            return ResponseEntity.ok(Map.of(
                "message", "User attendance data retrieved successfully",
                "userId", userId,
                "weekStart", nextWeekStart.toString(),
                "data", attendanceData
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> saveAttendance(@RequestBody Map<String, Object> request) {
        try {
            Object userIdObj = request.get("userId");
            String userId;
            
            // userId Long veya String olabilir, String'e çevir
            if (userIdObj instanceof Long) {
                userId = userIdObj.toString();
            } else if (userIdObj instanceof String) {
                userId = (String) userIdObj;
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid userId format"
                ));
            }
            
            String weekStart = (String) request.get("weekStart");
            @SuppressWarnings("unchecked")
            List<Integer> dates = (List<Integer>) request.get("dates");
            
            if (userId == null || weekStart == null || dates == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields: userId, weekStart, or dates"
                ));
            }
            
            LocalDate weekStartDate = LocalDate.parse(weekStart);
            attendanceService.record(userId, weekStartDate, dates);
            
            return ResponseEntity.ok(Map.of(
                "message", "Attendance saved successfully",
                "userId", userId,
                "weekStart", weekStart
            ));
        } catch (Exception e) {
            e.printStackTrace(); // Console'a detaylı hata yazdır
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to save attendance: " + e.getMessage(),
                "details", e.toString()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateAttendance(@PathVariable String id, @RequestBody Map<String, Object> request) {
        try {
            Object userIdObj = request.get("userId");
            String userId;
            
            // userId Long veya String olabilir, String'e çevir
            if (userIdObj instanceof Long) {
                userId = userIdObj.toString();
            } else if (userIdObj instanceof String) {
                userId = (String) userIdObj;
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid userId format"
                ));
            }
            
            String weekStart = (String) request.get("weekStart");
            @SuppressWarnings("unchecked")
            List<Integer> dates = (List<Integer>) request.get("dates");
            
            if (userId == null || weekStart == null || dates == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields: userId, weekStart, or dates"
                ));
            }
            
            LocalDate weekStartDate = LocalDate.parse(weekStart);
            attendanceService.record(userId, weekStartDate, dates);
            
            return ResponseEntity.ok(Map.of(
                "message", "Attendance updated successfully",
                "id", id,
                "userId", userId,
                "weekStart", weekStart
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to update attendance: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approveAttendance(@PathVariable Long id) {
        try {
            attendanceService.approve(id, "admin");
            return ResponseEntity.ok(Map.of(
                "message", "Attendance approved successfully",
                "id", id
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to approve attendance: " + e.getMessage()
            ));
        }
    }
} 