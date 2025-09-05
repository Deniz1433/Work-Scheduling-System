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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(AttendanceController.class);

    private final AttendanceService service;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final CustomAnnotationEvaluator permissionEvaluator;

    @Autowired
    public AttendanceController(AttendanceService service,
                                UserRepository userRepository,
                                EmailService emailService,
                                CustomAnnotationEvaluator permissionEvaluator) {
        this.service = service;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.permissionEvaluator = permissionEvaluator;
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        String keycloakId = principal.getName();
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found for keycloak ID: " + keycloakId));
        return user.getId();
    }

    // Public endpoint - user records their own attendance
    @PostMapping
    public ResponseEntity<?> submit(
            @RequestBody AttendanceRequest req,
            Principal principal
    ) {
        try {
            Long userId = getUserIdFromPrincipal(principal);
            service.record(userId, LocalDate.parse(req.getWeekStart()), req.getDates());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Error submitting attendance", e);
            return ResponseEntity.internalServerError().body("Error submitting attendance");
        }
    }

    // Edit another user's attendance (requires permissions)
    @PreAuthorize("@CustomAnnotationEvaluator.hasAnyPermission(authentication, {'ADMIN_ALL', 'EDIT_CHILD_ATTENDANCE', 'EDIT_ALL_ATTENDANCE', 'EDIT_DEPARTMENT_ATTENDANCE'})")
    @PostMapping("/{id}")
    public ResponseEntity<?> submit(@PathVariable Long id, @RequestBody AttendanceRequest req, Principal principal) {
        logger.info("Controller received request - ID: {}, Principal: {}", id, principal.getName());
        logger.info("Request body - weekStart: {}, dates: {}", req.getWeekStart(), req.getDates());

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long targetUserId = Long.parseLong(req.getUserId());

            // Prevent self-editing from team view
            Long currentUserId = getUserIdFromPrincipal(principal);
            if (currentUserId.equals(targetUserId)) {
                return ResponseEntity.status(403).body(Map.of("error", "You cannot edit your own attendance from the team view"));
            }

            if (!permissionEvaluator.canEditAttendance(authentication, targetUserId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Insufficient permissions to edit this user's attendance"));
            }

            service.record(targetUserId, LocalDate.parse(req.getWeekStart()), req.getDates());
            logger.info("Service call completed successfully");

            // Send email notification if explanation is provided
            if (req.getExplanation() != null && !req.getExplanation().trim().isEmpty()) {
                try {
                    User targetUser = userRepository.findById(targetUserId).orElse(null);
                    User editor = userRepository.findByKeycloakId(principal.getName()).orElse(null);

                    if (targetUser != null && editor != null) {
                        String body = "Ofis günleriniz " + editor.getFirstName() + " " + editor.getLastName() + " tarafından düzenlenmiştir. \nAçıklama: " + req.getExplanation() + "\nLütfen kontrol ediniz.";
                        emailService.sendEmail(targetUser.getEmail(), "Ofis günleriniz düzenlendi", body);
                        logger.info("Email sent successfully to: {}", targetUser.getEmail());
                    }
                } catch (Exception emailError) {
                    logger.error("Error sending email", emailError);
                    // Email error shouldn't affect attendance update
                }
            }

            return ResponseEntity.ok(Map.of("message", "Attendance updated successfully", "userId", id));
        } catch (Exception e) {
            logger.error("Error in controller", e);
            return ResponseEntity.internalServerError().body("Error processing request: " + e.getMessage());
        }
    }

    @PostMapping("/{userId}/{weekStart}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long userId,
            @PathVariable String weekStart,
            Principal principal) {

        logger.info("Attendance approval request by user: {}", principal.getName());

        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Require permission; allow self if they have it
            if (!permissionEvaluator.canApproveAttendance(authentication, userId)
                    && !permissionEvaluator.canEditAttendance(authentication, userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Insufficient permissions to approve this user's attendance"));
            }

            // (Optional) If you still want to disallow self for non-admins, the above check already handles it.

            service.approve(userId, LocalDate.parse(weekStart));
            logger.info("Attendance approved successfully!");
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error in approve", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error processing approval: " + e.getMessage()));
        }
    }


    @PostMapping("/excuse/{id}/approve")
    public ResponseEntity<?> approveExcuse(@PathVariable Long id) {
        try {
            Excuse excuse = service.getExcuseById(id);
            if (excuse == null) {
                return ResponseEntity.notFound().build();
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // Allow approving own excuse if the user has approval/edit permission
            boolean hasApprovalPermission =
                    permissionEvaluator.canApproveAttendance(authentication, excuse.getUserId())
                            || permissionEvaluator.canEditAttendance(authentication, excuse.getUserId());

            if (!hasApprovalPermission) {
                return ResponseEntity.status(403).body(Map.of("error", "Insufficient permissions to approve this user's excuse"));
            }

            service.approveExcuse(id);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error in approveExcuse", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Error processing excuse approval: " + e.getMessage()));
        }
    }


    // Public endpoint
    @GetMapping("/{weekStart}")
    public ResponseEntity<ArrayList<Object>> getAttendanceData(Principal principal, @PathVariable String weekStart) {
        try {
            Long userId = getUserIdFromPrincipal(principal);
            ArrayList<Object> attendanceResponse = service.fetch(userId, LocalDate.parse(weekStart));
            return ResponseEntity.ok(attendanceResponse);
        } catch (Exception e) {
            logger.error("Error getting attendance data", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/team")
    public ResponseEntity<List<TeamAttendanceDto>> getTeamAttendance(
            Principal principal,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String roleId,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String workStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String weekStart
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            logger.info("Controller received workStatus: {}", workStatus);

            List<TeamAttendanceDto> team = service.getTeamAttendanceWithFiltersAndPermissions(
                    principal.getName(),
                    authentication,
                    departmentId,
                    roleId,
                    searchTerm,
                    workStatus,
                    startDate,
                    endDate,
                    weekStart
            );
            return ResponseEntity.ok(team);
        } catch (Exception e) {
            logger.error("Error in getTeamAttendance", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/excuse/{id}")
    public ResponseEntity<List<ExcuseDto>> getExcuse(Principal principal, @PathVariable Long id) {
        try {
            getUserIdFromPrincipal(principal);
            List<Excuse> excuses = service.getExcuse(id);
            List<ExcuseDto> excuseDtos = excuses.stream()
                    .map(e -> new ExcuseDto(e.getId(), e.getUserId(), e.getExcuseDate().toString(), e.getExcuseType(), e.getDescription(), e.getIsApproved()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(excuseDtos);
        } catch (Exception e) {
            logger.error("Error getting excuses", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user-permissions")
    public ResponseEntity<Map<String, Object>> getUserPermissions(Principal principal) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            User user = userRepository.findByKeycloakId(principal.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            boolean canViewAll = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "VIEW_ALL_ATTENDANCE"});
            boolean canViewChild = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "VIEW_CHILD_ATTENDANCE"});
            boolean canViewDepartment = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "VIEW_DEPARTMENT_ATTENDANCE"});

            boolean canViewAllUsers = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "VIEW_ALL_USERS"});
            boolean canViewAllDepartments = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "VIEW_ALL_DEPARTMENTS"});
            boolean canViewRoles = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "VIEW_ROLES"});
            boolean canViewHolidays = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "VIEW_HOLIDAYS"});
            boolean canViewDepartmentHierarchy = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "VIEW_DEPARTMENT_HIERARCHY"});
            boolean canViewSurveys = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "VIEW_SURVEYS"});
            boolean canManageSurveys = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "MANAGE_SURVEYS"});

            Map<String, Object> permissions = new HashMap<>();
            permissions.put("canViewAll", canViewAll);
            permissions.put("canViewChild", canViewChild);
            permissions.put("canViewDepartment", canViewDepartment);
            permissions.put("userDepartmentId", user.getDepartment() != null ? user.getDepartment().getId() : "");
            permissions.put("userDepartmentName", user.getDepartment() != null ? user.getDepartment().getName() : "");
            permissions.put("canViewAllUsers", canViewAllUsers);
            permissions.put("canViewAllDepartments", canViewAllDepartments);
            permissions.put("canViewRoles", canViewRoles);
            permissions.put("canViewHolidays", canViewHolidays);
            permissions.put("canViewDepartmentHierarchy", canViewDepartmentHierarchy);
            permissions.put("canViewSurveys", canViewSurveys);
            permissions.put("canManageSurveys", canManageSurveys);

            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            logger.error("Error getting user permissions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/edit-permissions")
    public ResponseEntity<Map<String, Boolean>> getEditPermissions(Principal principal) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            User user = userRepository.findByKeycloakId(principal.getName()).orElse(null);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            boolean canEditAll = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "EDIT_ALL_ATTENDANCE"});
            boolean canEditChild = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "EDIT_CHILD_ATTENDANCE"});
            boolean canEditDepartment = permissionEvaluator.hasAnyPermission(authentication, new String[]{"ADMIN_ALL", "EDIT_DEPARTMENT_ATTENDANCE"});

            Map<String, Boolean> editPermissions = Map.of(
                    "canEditAll", canEditAll,
                    "canEditChild", canEditChild,
                    "canEditDepartment", canEditDepartment
            );

            return ResponseEntity.ok(editPermissions);
        } catch (Exception e) {
            logger.error("Error getting edit permissions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/check-edit-permissions")
    public ResponseEntity<Map<String, Boolean>> checkEditPermissionsForUsers(
            @RequestBody List<Long> userIds
    ) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            Map<String, Boolean> permissions = new HashMap<>();

            for (Long userId : userIds) {
                boolean canEdit = permissionEvaluator.canEditAttendance(authentication, userId);
                permissions.put(userId.toString(), canEdit);
            }

            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            logger.error("Error checking edit permissions", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserAttendance(@PathVariable Long userId) {
        try {
            List<Attendance> attendances = service.getAttendanceByUserId(userId);

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
            logger.error("Error getting user attendance", e);
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
            logger.error("Error updating user attendance", e);
            return ResponseEntity.internalServerError().body("Error updating attendance data");
        }
    }
}