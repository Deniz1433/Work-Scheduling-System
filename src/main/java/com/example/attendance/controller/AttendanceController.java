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
            System.out.println("=== GET ATTENDANCE REQUEST ===");
            System.out.println("Requested weekStart: " + weekStart);
            
            // Kullanıcının kendi attendance verilerini getir
            if (weekStart == null) {
                // Gelecek haftanın başlangıç tarihini hesapla
                LocalDate nextWeekStart = LocalDate.now();
                int dayOfWeek = nextWeekStart.getDayOfWeek().getValue();
                int daysUntilNextMonday = (8 - dayOfWeek) % 7;
                nextWeekStart = nextWeekStart.plusDays(daysUntilNextMonday);
                weekStart = nextWeekStart.toString();
                System.out.println("Calculated weekStart: " + weekStart);
            }
            
            // Test kullanıcısı için gerçek veritabanından veri çek
            String testUserId = "d5478a21-ee0b-400b-bbee-3c155c4a0d56"; // Test kullanıcısının Keycloak ID'si
            System.out.println("Fetching attendance for userId: " + testUserId + ", weekStart: " + weekStart);
            
            // Önce tüm attendance kayıtlarını kontrol et
            var allAttendance = attendanceService.getAllAttendance();
            System.out.println("All attendance records in DB: " + allAttendance);
            
            var attendanceData = attendanceService.fetch(testUserId, weekStart);
            System.out.println("Fetched attendance data: " + attendanceData);
            
            return ResponseEntity.ok(Map.of(
                "message", "Attendance data retrieved successfully",
                "weekStart", weekStart,
                "data", attendanceData,
                "allRecords", allAttendance
            ));
        } catch (Exception e) {
            System.out.println("=== GET ATTENDANCE ERROR ===");
            e.printStackTrace();
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
            System.out.println("=== GET USER ATTENDANCE ===");
            System.out.println("Requested userId: " + userId);
            
            // Kullanıcının tüm attendance kayıtlarını getir
            var allUserAttendance = attendanceService.getAttendanceByUserId(userId);
            System.out.println("All attendance records for user: " + allUserAttendance);
            
            // Attendance kayıtlarını frontend'in beklediği formata dönüştür
            var attendanceRecords = allUserAttendance.stream()
                .map(attendance -> {
                    var record = Map.of(
                        "weekStart", attendance.getWeekStart(),
                        "monday", attendance.getMonday(),
                        "tuesday", attendance.getTuesday(),
                        "wednesday", attendance.getWednesday(),
                        "thursday", attendance.getThursday(),
                        "friday", attendance.getFriday(),
                        "isApproved", attendance.isApproved()
                    );
                    return record;
                })
                .toList();
            
            System.out.println("Formatted attendance records: " + attendanceRecords);
            
            return ResponseEntity.ok(Map.of(
                "message", "User attendance data retrieved successfully",
                "userId", userId,
                "data", Map.of(
                    "attendanceRecords", attendanceRecords
                )
            ));
        } catch (Exception e) {
            System.out.println("=== GET USER ATTENDANCE ERROR ===");
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to get user attendance: " + e.getMessage()
            ));
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> saveAttendance(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== ATTENDANCE SAVE REQUEST ===");
            System.out.println("Request body: " + request);
            
            Object userIdObj = request.get("userId");
            String userId;
            
            // userId herhangi bir tip olabilir, String'e çevir
            if (userIdObj == null) {
                System.out.println("userId is null");
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "userId is required"
                ));
            }
            
            userId = userIdObj.toString();
            System.out.println("Converted userId: " + userId + " (original: " + userIdObj + ", type: " + userIdObj.getClass().getName() + ")");
            
            String weekStart = (String) request.get("weekStart");
            @SuppressWarnings("unchecked")
            List<Integer> dates = (List<Integer>) request.get("dates");
            
            System.out.println("Parsed values - userId: " + userId + ", weekStart: " + weekStart + ", dates: " + dates);
            
            if (userId == null || weekStart == null || dates == null) {
                System.out.println("Missing required fields - userId: " + userId + ", weekStart: " + weekStart + ", dates: " + dates);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing required fields: userId, weekStart, or dates"
                ));
            }
            
            LocalDate weekStartDate = LocalDate.parse(weekStart);
            System.out.println("Calling attendanceService.record with: " + userId + ", " + weekStartDate + ", " + dates);
            
            attendanceService.record(userId, weekStartDate, dates);
            
            // Kaydetme sonrası veritabanını kontrol et
            var allAttendance = attendanceService.getAllAttendance();
            System.out.println("All attendance records after save: " + allAttendance);
            
            System.out.println("Attendance saved successfully!");
            return ResponseEntity.ok(Map.of(
                "message", "Attendance saved successfully",
                "userId", userId,
                "weekStart", weekStart,
                "allRecords", allAttendance
            ));
        } catch (Exception e) {
            System.out.println("=== ATTENDANCE SAVE ERROR ===");
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
            
            // userId herhangi bir tip olabilir, String'e çevir
            if (userIdObj == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "userId is required"
                ));
            }
            
            userId = userIdObj.toString();
            
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

    @PutMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> updateUserAttendance(@PathVariable String userId, @RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== UPDATE USER ATTENDANCE ===");
            System.out.println("Requested userId: " + userId);
            System.out.println("Request body: " + request);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attendanceRecords = (List<Map<String, Object>>) request.get("attendanceRecords");
            
            if (attendanceRecords == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "attendanceRecords is required"
                ));
            }
            
            // Her attendance kaydını güncelle
            for (Map<String, Object> record : attendanceRecords) {
                String weekStart = (String) record.get("weekStart");
                Integer monday = (Integer) record.get("monday");
                Integer tuesday = (Integer) record.get("tuesday");
                Integer wednesday = (Integer) record.get("wednesday");
                Integer thursday = (Integer) record.get("thursday");
                Integer friday = (Integer) record.get("friday");
                
                if (weekStart != null) {
                    LocalDate weekStartDate = LocalDate.parse(weekStart);
                    List<Integer> dates = List.of(monday, tuesday, wednesday, thursday, friday);
                    attendanceService.record(userId, weekStartDate, dates);
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "User attendance updated successfully",
                "userId", userId
            ));
        } catch (Exception e) {
            System.out.println("=== UPDATE USER ATTENDANCE ERROR ===");
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Failed to update user attendance: " + e.getMessage()
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
    
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugAttendance() {
        try {
            System.out.println("=== DEBUG ATTENDANCE ===");
            
            // Tüm attendance kayıtlarını listele
            var allAttendance = attendanceService.getAllAttendance();
            System.out.println("All attendance records: " + allAttendance);
            
            // Test kullanıcısı için attendance ara
            String testUserId = "d5478a21-ee0b-400b-bbee-3c155c4a0d56";
            var userAttendance = attendanceService.getAttendanceByUserId(testUserId);
            System.out.println("User attendance records: " + userAttendance);
            
            return ResponseEntity.ok(Map.of(
                "message", "Debug info retrieved",
                "allAttendance", allAttendance,
                "userAttendance", userAttendance
            ));
        } catch (Exception e) {
            System.out.println("=== DEBUG ERROR ===");
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Debug failed: " + e.getMessage()
            ));
        }
    }
} 