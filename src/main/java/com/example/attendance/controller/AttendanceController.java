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
import com.example.attendance.security.CustomAnnotationEvaluator;

import jakarta.ws.rs.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @Autowired
    private CustomAnnotationEvaluator permissionEvaluator;


    private Long getUserIdFromPrincipal(Principal principal) {
        String keycloakId = principal.getName();
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found for keycloak ID: " + keycloakId));
        return user.getId();
    }

    //herkese açık
    //kendi attendance'ını kaydetme 
    @PostMapping
    public ResponseEntity<?> submit(
            @RequestBody AttendanceRequest req,
            Principal principal
    ) {
        Long userId = getUserIdFromPrincipal(principal);
        service.record(userId, LocalDate.parse(req.getWeekStart()), req.getDates());
        return ResponseEntity.ok().build();
    }

    //başka birinin attendance bilgisini düzenleme
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, null, {'ADMIN_ALL', 'EDIT_CHILD_ATTENDANCE', 'EDIT_ALL_ATTENDANCE', 'EDIT_DEPARTMENT_ATTENDANCE'})")
    @PostMapping("/{id}")
    public ResponseEntity<?> submit(@PathVariable Long id, @RequestBody AttendanceRequest req, Principal principal) {
        System.out.println("Controller received request - ID: " + id + ", Principal: " + principal.getName());
        System.out.println("Request body - weekStart: " + req.getWeekStart() + ", dates: " + req.getDates());
    
        try {
            // Yetki kontrolü yap
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long targetUserId = Long.parseLong(req.getUserId());

            // ❌ Team view üzerinden kendini düzenleme yasak
            Long currentUserId = getUserIdFromPrincipal(principal);
            if (currentUserId.equals(targetUserId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You cannot edit your own attendance from the team view"));
            }

            if (!permissionEvaluator.canEditAttendance(authentication, targetUserId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Insufficient permissions to edit this user's attendance"));
            }


            // Frontend'den gelen userId'yi kullan, principal.getName() değil
            service.record(Long.parseLong(req.getUserId()), LocalDate.parse(req.getWeekStart()), req.getDates());
            System.out.println("Service call completed successfully");
            
            // E-posta gönderme işlemi (eğer explanation varsa)
            if (req.getExplanation() != null && !req.getExplanation().trim().isEmpty()) {
                try {
                    User targetUser = userRepository.findById(targetUserId).orElse(null);
                    User editor = userRepository.findByKeycloakId(principal.getName()).orElse(null);
                    
                    if (targetUser != null && editor != null) {
                        String body = "Ofis günleriniz " + editor.getFirstName() + " " + editor.getLastName() + " tarafından düzenlenmiştir. \nAçıklama: " + req.getExplanation() + "\nLütfen kontrol ediniz.";
                        emailService.sendEmail(targetUser.getEmail(), "Ofis günleriniz düzenlendi", body);
                        System.out.println("Email sent successfully to: " + targetUser.getEmail());
                    }
                } catch (Exception emailError) {
                    System.err.println("Error sending email: " + emailError.getMessage());
                    // E-posta hatası attendance güncellemesini etkilemesin
                }
            }
            
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
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // ❌ Team view üzerinden kendi attendance'ını onaylama yasak
            Long currentUserId = getUserIdFromPrincipal(principal);
            if (currentUserId.equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You cannot approve your own attendance from the team view"));
            }

            if (!permissionEvaluator.canApproveAttendance(authentication, userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Insufficient permissions to approve this user's attendance"));
            }


            service.approve(userId, LocalDate.parse(weekStart));
            System.out.println("✅ Attendance approved successfully!");
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error in approve: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error processing approval: " + e.getMessage());
        }
    }

    @PostMapping("/excuse/{id}/approve")
    public ResponseEntity<?> approveExcuse(@PathVariable Long id, Principal principal) {
        try {
            // Önce excuse'ı bul ve hangi kullanıcıya ait olduğunu öğren
            Excuse excuse = service.getExcuseById(id);
            if (excuse == null) {
                return ResponseEntity.notFound().build();
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // ❌ Team view üzerinden kendi mazeretini onaylama yasak
            Long currentUserId = getUserIdFromPrincipal(principal);
            if (currentUserId.equals(excuse.getUserId())) {
                return ResponseEntity.status(403).body(Map.of("error", "You cannot approve your own excuse from the team view"));
            }

            if (!permissionEvaluator.canApproveAttendance(authentication, excuse.getUserId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Insufficient permissions to approve this user's excuse"));
            }


            Long userId = getUserIdFromPrincipal(principal);
            service.approveExcuse(id, userId.toString());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error in approveExcuse: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error processing excuse approval: " + e.getMessage());
        }
    }

    //herkese açık
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
        try {
            String keycloakId = principal.getName();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Yetki kontrolü - hangi kullanıcıları görebileceğini belirle
            List<TeamAttendanceDto> team = service.getTeamAttendanceWithFiltersAndPermissions(
                keycloakId,
                authentication,
                departmentId, 
                roleId, 
                searchTerm
            );
            return ResponseEntity.ok(team);
        } catch (Exception e) {
            System.err.println("Error in getTeamAttendance: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
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

    @GetMapping("/user-permissions")
    public ResponseEntity<Map<String, Object>> getUserPermissions(Principal principal) {
        try {
            String keycloakId = principal.getName();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Kullanıcıyı bul
            User user = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Attendance görüntüleme yetkileri
            boolean canViewAll = permissionEvaluator.hasAnyPermission(authentication, null, new String[]{"ADMIN_ALL", "VIEW_ALL_ATTENDANCE"});
            boolean canViewChild = permissionEvaluator.hasAnyPermission(authentication, null, new String[]{"ADMIN_ALL", "VIEW_CHILD_ATTENDANCE"});
            boolean canViewDepartment = permissionEvaluator.hasAnyPermission(authentication, null, new String[]{"ADMIN_ALL", "VIEW_DEPARTMENT_ATTENDANCE"});
            
            // Navigasyon yetkileri
            boolean canViewAllUsers = permissionEvaluator.hasAnyPermission(authentication, null, new String[]{"ADMIN_ALL", "VIEW_ALL_USERS"});
            boolean canViewAllDepartments = permissionEvaluator.hasAnyPermission(authentication, null, new String[]{"ADMIN_ALL", "VIEW_ALL_DEPARTMENTS"});
            boolean canViewRoles = permissionEvaluator.hasAnyPermission(authentication, null, new String[]{"ADMIN_ALL", "VIEW_ROLES"});
            boolean canViewHolidays = permissionEvaluator.hasAnyPermission(authentication, null, new String[]{"ADMIN_ALL", "VIEW_HOLIDAYS"});
            boolean canViewDepartmentHierarchy = permissionEvaluator.hasAnyPermission(authentication, null, new String[]{"ADMIN_ALL", "VIEW_DEPARTMENT_HIERARCHY"});
            
            Map<String, Object> permissions = Map.of(
                "canViewAll", canViewAll,
                "canViewChild", canViewChild,
                "canViewDepartment", canViewDepartment,
                "userDepartmentId", user.getDepartment() != null ? user.getDepartment().getId() : null,
                "userDepartmentName", user.getDepartment() != null ? user.getDepartment().getName() : null,
                "canViewAllUsers", canViewAllUsers,
                "canViewAllDepartments", canViewAllDepartments,
                "canViewRoles", canViewRoles,
                "canViewHolidays", canViewHolidays,
                "canViewDepartmentHierarchy", canViewDepartmentHierarchy
            );
            
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            System.err.println("Error getting user permissions: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/edit-permissions")
    public ResponseEntity<Map<String, Boolean>> getEditPermissions(Principal principal) {
        try {
            String keycloakId = principal.getName();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // Kullanıcıyı bul
            User user = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Edit yetkilerini kontrol et
            boolean canEditAll = permissionEvaluator.hasAnyPermission(authentication, null, new String[]{"ADMIN_ALL", "EDIT_ALL_ATTENDANCE"});
            boolean canEditChild = permissionEvaluator.hasAnyPermission(authentication, null, new String[]{"ADMIN_ALL", "EDIT_CHILD_ATTENDANCE"});
            boolean canEditDepartment = permissionEvaluator.hasAnyPermission(authentication, null, new String[]{"ADMIN_ALL", "EDIT_DEPARTMENT_ATTENDANCE"});
            
            Map<String, Boolean> editPermissions = Map.of(
                "canEditAll", canEditAll,
                "canEditChild", canEditChild,
                "canEditDepartment", canEditDepartment
            );
            
            return ResponseEntity.ok(editPermissions);
        } catch (Exception e) {
            System.err.println("Error getting edit permissions: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/check-edit-permissions")
    public ResponseEntity<Map<String, Boolean>> checkEditPermissionsForUsers(
            Principal principal, 
            @RequestBody List<Long> userIds
    ) {
        try {
            String keycloakId = principal.getName();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            Map<String, Boolean> permissions = new HashMap<>();
            
            for (Long userId : userIds) {
                boolean canEdit = permissionEvaluator.canEditAttendance(authentication, userId);
                permissions.put(userId.toString(), canEdit);
            }
            
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            System.err.println("Error checking edit permissions: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserAttendance(@PathVariable Long userId) {
        try {
            // Kullanıcının tüm attendance kayıtlarını getir
            List<Attendance> attendances = service.getAttendanceByUserId(userId);
            
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