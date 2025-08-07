package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceRequest;
import com.example.attendance.dto.ExcuseDto;
import com.example.attendance.dto.TeamAttendanceDto;
import com.example.attendance.model.Attendance;
import com.example.attendance.model.Excuse;
import com.example.attendance.model.User;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.EmailService;

import jakarta.ws.rs.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    @Autowired
    private AttendanceService service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailService emailService;



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

    @PostMapping("/{id}/editEmail")
    public ResponseEntity<?> sendEmail(@PathVariable Long id, @RequestBody String explanation ,Principal principal){
        User u = userRepository.findById(id).orElse(null);
        User editor = userRepository.findByKeycloakId(principal.getName()).orElse(null);
        if(u != null && editor != null){
            try{
                String explanationText = explanation.toString().replaceAll(".*\"explanation\":\"([^\"]+)\".*", "$1");
                String body = "Ofis günleriniz " + editor.getFirstName() + " " + editor.getLastName() + " tarafından düzenlenmiştir. \nAçıklama: " + explanationText + "\nLütfen kontrol ediniz.";
                emailService.sendEmail(u.getEmail(), "Ofis günleriniz düzenlendi", body);
                return ResponseEntity.ok(Map.of("message", "Email sent successfully", "userId", id));
            }
            catch (Exception e) {
                System.err.println("Error in controller: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.internalServerError().body("Error processing request: " + e.getMessage());
            }
        }
        else{
            return ResponseEntity.internalServerError().body("User not found!");
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

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserAttendance(@PathVariable Long userId) {
        try {
            // Kullanıcının tüm attendance kayıtlarını getir
            List<Attendance> attendances = service.getUserAttendance(userId);
            
            // DTO formatına dönüştür
            List<Map<String, Object>> attendanceRecords = attendances.stream()
                .map(attendance -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("weekStart", attendance.getWeekStart().toString());
                    record.put("monday", attendance.getMonday());
                    record.put("tuesday", attendance.getTuesday());
                    record.put("wednesday", attendance.getWednesday());
                    record.put("thursday", attendance.getThursday());
                    record.put("friday", attendance.getFriday());
                    record.put("isApproved", attendance.isApproved());
                    return record;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("attendanceRecords", attendanceRecords);
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error getting user attendance: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error getting attendance data");
        }
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<?> updateUserAttendance(@PathVariable Long userId, @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attendanceRecords = (List<Map<String, Object>>) request.get("attendanceRecords");
            
            for (Map<String, Object> record : attendanceRecords) {
                String weekStart = (String) record.get("weekStart");
                int monday = (Integer) record.get("monday");
                int tuesday = (Integer) record.get("tuesday");
                int wednesday = (Integer) record.get("wednesday");
                int thursday = (Integer) record.get("thursday");
                int friday = (Integer) record.get("friday");
                
                List<Integer> dates = List.of(monday, tuesday, wednesday, thursday, friday);
                service.record(userId, LocalDate.parse(weekStart), dates);
            }
            
            return ResponseEntity.ok("Attendance records updated successfully");
        } catch (Exception e) {
            System.err.println("Error updating user attendance: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error updating attendance data");
        }
    }
}