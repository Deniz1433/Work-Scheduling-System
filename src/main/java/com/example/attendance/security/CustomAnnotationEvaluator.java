package com.example.attendance.security;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.example.attendance.model.Department;
import com.example.attendance.model.Role;
import com.example.attendance.model.RolePermission;
import com.example.attendance.model.User;
import com.example.attendance.repository.RolePermissionRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.service.DepartmentHierarchyService;

@Component("CustomAnnotationEvaluator")
public class CustomAnnotationEvaluator implements PermissionEvaluator {

      @Autowired private RolePermissionRepository rolePermissionRepository;
      @Autowired private UserRepository userRepository;
      @Autowired private DepartmentHierarchyService departmentHierarchyService;

      @Override
      public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
            return checkPermission(authentication, permission);
      }

      @Override
      public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
            return checkPermission(authentication, permission);
      }

      /** Shared permission check: first JWT authorities, then DB fallback. */
      private boolean checkPermission(Authentication authentication, Object permission) {
            if (authentication == null || permission == null) return false;
            String required = permission.toString();

            // 1) JWT-driven: PERM_{required}
            if (hasJwtPermission(authentication, required)) return true;

            // 2) Fallback to DB role->permissions
            String keycloakId = authentication.getName(); // Keycloak subject/username depending on config
            User user = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (user == null || user.getRole() == null) return false;

            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(user.getRole().getId());
            return rolePermissions.stream().anyMatch(rp -> required.equals(rp.getPermission().getName()));
      }

      private boolean hasJwtPermission(Authentication auth, String perm) {
            String needed = "PERM_" + perm;
            for (GrantedAuthority ga : auth.getAuthorities()) {
                  String a = ga.getAuthority();
                  if (needed.equals(a)) return true;
                  // Treat superadmin roles as full access
                  if ("ROLE_attendance_client_superadmin".equalsIgnoreCase(a) || "ROLE_realm_superadmin".equalsIgnoreCase(a)) {
                        return true;
                  }
            }
            return false;
      }

      // Any-of
      public boolean hasAnyPermission(Authentication authentication, Object targetDomainObject, Object permissions) {
            String[] required = toArray(permissions);
            if (required == null) return false;

            // 1) JWT-driven ANY
            for (String p : required) {
                  if (hasJwtPermission(authentication, p)) return true;
            }

            // 2) DB fallback
            String keycloakId = authentication.getName();
            User user = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (user == null || user.getRole() == null) return false;

            List<String> userPerms = rolePermissionRepository.findByRoleId(user.getRole().getId()).stream()
                    .map(rp -> rp.getPermission().getName())
                    .collect(Collectors.toList());

            for (String p : required) {
                  if (userPerms.contains(p)) return true;
            }
            return false;
      }

      // All-of
      public boolean hasAllPermissions(Authentication authentication, Object targetDomainObject, Object permissions) {
            String[] required = toArray(permissions);
            if (required == null) return false;

            // 1) JWT-driven ALL
            boolean allJwt = true;
            for (String p : required) {
                  if (!hasJwtPermission(authentication, p)) { allJwt = false; break; }
            }
            if (allJwt) return true;

            // 2) DB fallback
            String keycloakId = authentication.getName();
            User user = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (user == null || user.getRole() == null) return false;

            Set<String> userPerms = rolePermissionRepository.findByRoleId(user.getRole().getId()).stream()
                    .map(rp -> rp.getPermission().getName())
                    .collect(Collectors.toSet());

            for (String p : required) {
                  if (!userPerms.contains(p)) return false;
            }
            return true;
      }

      // Attendance view/edit helpers unchanged below (but they now benefit from JWT checks via hasAnyPermission calls where used)

      public boolean canViewAttendance(Authentication authentication, Long targetUserId) {
            String keycloakId = authentication.getName();
            User viewer = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (viewer == null) return false;

            User targetUser = userRepository.findById(targetUserId).orElse(null);
            if (targetUser == null) return false;

            if (viewer.getId().equals(targetUserId)) return true;

            // Admin/All via JWT or DB
            if (hasAnyPermission(authentication, null, List.of("ADMIN_ALL", "VIEW_ALL_ATTENDANCE"))) return true;

            // Child / Department checks (DB-driven context)
            if (hasAnyPermission(authentication, null, List.of("VIEW_CHILD_ATTENDANCE"))) {
                  System.out.println("🔍 canViewAttendance: User has VIEW_CHILD_ATTENDANCE permission");
                  System.out.println("🔍 canViewAttendance: Viewer: " + viewer.getUsername() + " (dept: " + (viewer.getDepartment() != null ? viewer.getDepartment().getName() : "null") + ")");
                  System.out.println("🔍 canViewAttendance: Target: " + targetUser.getUsername() + " (dept: " + (targetUser.getDepartment() != null ? targetUser.getDepartment().getName() : "null") + ")");
                  return isInChildDepartments(viewer.getDepartment(), targetUser.getDepartment());
            }
            if (hasAnyPermission(authentication, null, List.of("VIEW_DEPARTMENT_ATTENDANCE"))) {
                  return viewer.getDepartment() != null && targetUser.getDepartment() != null
                          && Objects.equals(viewer.getDepartment().getId(), targetUser.getDepartment().getId());
            }

            return false;
      }

      public boolean canEditAttendance(Authentication authentication, Long targetUserId) {
            String keycloakId = authentication.getName();
            User editor = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (editor == null) return false;

            User targetUser = userRepository.findById(targetUserId).orElse(null);
            if (targetUser == null) return false;

            if (editor.getId().equals(targetUserId)) return true;

            if (hasAnyPermission(authentication, null, List.of("ADMIN_ALL", "EDIT_ALL_ATTENDANCE"))) return true;

            if (hasAnyPermission(authentication, null, List.of("EDIT_CHILD_ATTENDANCE"))) {
                  return isInChildDepartments(editor.getDepartment(), targetUser.getDepartment());
            }
            if (hasAnyPermission(authentication, null, List.of("EDIT_DEPARTMENT_ATTENDANCE"))) {
                  return editor.getDepartment() != null && targetUser.getDepartment() != null
                          && Objects.equals(editor.getDepartment().getId(), targetUser.getDepartment().getId());
            }

            return false;
      }

      public boolean canApproveAttendance(Authentication authentication, Long targetUserId) {
            String keycloakId = authentication.getName();
            User actor = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (actor == null) return false;

            // For self: require the ability to approve/edit OTHERS (i.e., edit beyond self)
            if (actor.getId().equals(targetUserId)) {
                  return hasAnyPermission(authentication, null, List.of(
                          "ADMIN_ALL",
                          "EDIT_ALL_ATTENDANCE",
                          "EDIT_CHILD_ATTENDANCE",
                          "EDIT_DEPARTMENT_ATTENDANCE"
                  ));
            }

            // For others: same scope as edit-perms for that target
            return canEditAttendance(authentication, targetUserId);
      }

      private boolean isInChildDepartments(Department parentDepartment, Department childDepartment) {
            if (parentDepartment == null || childDepartment == null) {
                  System.out.println("🔍 isInChildDepartments: null departments - parent: " + parentDepartment + ", child: " + childDepartment);
                  return false;
            }
            
            // Same department = can view
            if (Objects.equals(parentDepartment.getId(), childDepartment.getId())) {
                  System.out.println("🔍 isInChildDepartments: same department - " + parentDepartment.getName());
                  return true;
            }
            
            try {
                  System.out.println("🔍 isInChildDepartments: checking hierarchy for parent: " + parentDepartment.getName() + " (id:" + parentDepartment.getId() + ")");
                  Set<Department> childDepartments = departmentHierarchyService.findAllDescendants(parentDepartment);
                  System.out.println("🔍 isInChildDepartments: found " + childDepartments.size() + " child departments");
                  
                  for (Department dept : childDepartments) {
                        System.out.println("🔍   - Child: " + dept.getName() + " (id:" + dept.getId() + ")");
                  }
                  
                  // Check by ID instead of contains() to be extra sure
                  boolean result = childDepartments.stream()
                        .anyMatch(dept -> Objects.equals(dept.getId(), childDepartment.getId()));
                  
                  System.out.println("🔍 isInChildDepartments: target department " + childDepartment.getName() + " (id:" + childDepartment.getId() + ") is child: " + result);
                  return result;
            } catch (Exception e) {
                  System.out.println("❌ isInChildDepartments: exception - " + e.getMessage());
                  e.printStackTrace();
                  return false;
            }
      }

      private String[] toArray(Object permissions) {
            if (permissions instanceof String[]) return (String[]) permissions;
            if (permissions instanceof List<?> list) {
                  return list.stream().map(String::valueOf).toArray(String[]::new);
            }
            return null;
      }
}
