package com.example.attendance.security;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
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

      @Autowired
      private RolePermissionRepository rolePermissionRepository;
      @Autowired
      private UserRepository userRepository;
      @Autowired
      private DepartmentHierarchyService departmentHierarchyService;

      @Override
      public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
            //authentication.getName() keycloak id'si döndürüyor
            String keycloakId = authentication.getName();
            String permissionName = permission.toString();
            User user = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (user == null) {
                  return false;
            }
            Role role = user.getRole();
            if(role == null){
                  return false;
            }
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(role.getId());
            return rolePermissions.stream().anyMatch(rp -> rp.getPermission().getName().equals(permissionName));
      }

      @Override
      public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType,
                  Object permission) {
            User user = userRepository.findById((Long)targetId).orElse(null);
            String permissionName = permission.toString();
            if (user == null) {
                  return false;
            }
            Role role = user.getRole();
            if(role == null){
                  return false;
            }
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(role.getId());
            return rolePermissions.stream().anyMatch(rp -> rp.getPermission().getName().equals(permissionName));
      }

      // permissionslardan herhangi birine sahipse true döndür
      public boolean hasAnyPermission(Authentication authentication, Object targetDomainObject, Object permissions) {
            try {
                  System.out.println("🔍 hasAnyPermission called with authentication: " + (authentication != null ? authentication.getName() : "null"));
                  System.out.println("🔍 permissions parameter: " + permissions);
                  
                  String keycloakId = authentication.getName();
                  System.out.println("🔍 keycloakId: " + keycloakId);
                  
                  User user = userRepository.findByKeycloakId(keycloakId).orElse(null);
                  if (user == null) {
                        System.out.println("❌ User not found for keycloakId: " + keycloakId);
                        return false;
                  }
                  System.out.println("✅ User found: " + user.getId());
                  
                  Role role = user.getRole();
                  if(role == null){
                        System.out.println("❌ User has no role assigned");
                        return false;
                  }
                  System.out.println("✅ User role: " + role.getName());
                  
                  // Handle both String array and List<String> cases
                  String[] requiredPermissions;
                  if (permissions instanceof String[]) {
                        requiredPermissions = (String[]) permissions;
                  } else if (permissions instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> permissionList = (java.util.List<String>) permissions;
                        requiredPermissions = permissionList.toArray(new String[0]);
                  } else {
                        System.err.println("❌ Unexpected permissions type: " + permissions.getClass().getName());
                        return false;
                  }
                  System.out.println("🔍 Required permissions: " + java.util.Arrays.toString(requiredPermissions));
                  
                  List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(role.getId());
                  System.out.println("🔍 Found " + rolePermissions.size() + " role permissions");
                  
                  List<String> userPermissions = rolePermissions.stream()
                        .map(rp -> rp.getPermission().getName())
                        .collect(Collectors.toList());
                  System.out.println("🔍 User permissions: " + userPermissions);
                  
                  for(String permission : requiredPermissions){
                        if(userPermissions.contains(permission)){
                              System.out.println("✅ User has permission: " + permission);
                              return true;
                        }
                  }
                  System.out.println("❌ User doesn't have any of the required permissions");
                  return false;
            } catch (Exception e) {
                  System.err.println("❌ Error in hasAnyPermission: " + e.getMessage());
                  e.printStackTrace();
                  return false;
            }
      }

      // permissionslardan hepsine sahipse true döndür, yoksa false döndür
      public boolean hasAllPermissions(Authentication authentication, Object targetDomainObject, Object permissions){
            User user = userRepository.findByKeycloakId(authentication.getName()).orElse(null);
            if(user == null){
                  return false;
            }
            Role role = user.getRole();
            if(role == null){
                  return false;
            }
            // Handle both String array and List<String> cases
            String[] requiredPermissions;
            if (permissions instanceof String[]) {
                  requiredPermissions = (String[]) permissions;
            } else if (permissions instanceof java.util.List) {
                  @SuppressWarnings("unchecked")
                  java.util.List<String> permissionList = (java.util.List<String>) permissions;
                  requiredPermissions = permissionList.toArray(new String[0]);
            } else {
                  System.err.println("❌ Unexpected permissions type: " + permissions.getClass().getName());
                  return false;
            }
            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(role.getId());
            List<String> userPermissions = rolePermissions.stream().map(rp -> rp.getPermission().getName()).collect(Collectors.toList());
            for(String permission : requiredPermissions){
                  if(userPermissions.contains(permission)){
                        continue;
                  }
                  else{
                        return false;
                  }
            }
            return true;
      }

      // Attendance görüntüleme yetkisi kontrolü
      public boolean canViewAttendance(Authentication authentication, Long targetUserId) {
            String keycloakId = authentication.getName();
            User viewer = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (viewer == null) {
                  System.out.println("❌ Viewer not found for keycloakId: " + keycloakId);
                  return false;
            }

            // Hedef kullanıcıyı bul
            User targetUser = userRepository.findById(targetUserId).orElse(null);
            if (targetUser == null) {
                  System.out.println("❌ Target user not found for userId: " + targetUserId);
                  return false;
            }

            // Kendi attendance'ını görebilir
            if (viewer.getId().equals(targetUserId)) {
                  System.out.println("✅ User can view their own attendance");
                  return true;
            }

            // Viewer'ın yetkilerini kontrol et
            Role viewerRole = viewer.getRole();
            if (viewerRole == null) {
                  System.out.println("❌ Viewer has no role assigned");
                  return false;
            }

            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(viewerRole.getId());
            List<String> userPermissions = rolePermissions.stream()
                  .map(rp -> rp.getPermission().getName())
                  .collect(Collectors.toList());

            System.out.println("🔍 Viewer permissions: " + userPermissions);
            System.out.println("🔍 Viewer department: " + (viewer.getDepartment() != null ? viewer.getDepartment().getName() : "null"));
            System.out.println("🔍 Target user department: " + (targetUser.getDepartment() != null ? targetUser.getDepartment().getName() : "null"));

            // ADMIN_ALL veya VIEW_ALL_ATTENDANCE yetkisi varsa direkt izin ver
            if (userPermissions.contains("ADMIN_ALL") || userPermissions.contains("VIEW_ALL_ATTENDANCE")) {
                  System.out.println("✅ User has ADMIN_ALL or VIEW_ALL_ATTENDANCE permission");
                  return true;
            }

            // VIEW_CHILD_ATTENDANCE yetkisi varsa, sadece child departmanlarındaki kullanıcıları görebilir
            if (userPermissions.contains("VIEW_CHILD_ATTENDANCE")) {
                  boolean isChild = isInChildDepartments(viewer.getDepartment(), targetUser.getDepartment());
                  System.out.println("🔍 VIEW_CHILD_ATTENDANCE check: " + isChild);
                  return isChild;
            }

            // VIEW_DEPARTMENT_ATTENDANCE yetkisi varsa, sadece kendi departmanındaki kullanıcıları görebilir
            if (userPermissions.contains("VIEW_DEPARTMENT_ATTENDANCE")) {
                  boolean sameDepartment = viewer.getDepartment().getId().equals(targetUser.getDepartment().getId());
                  System.out.println("🔍 VIEW_DEPARTMENT_ATTENDANCE check: " + sameDepartment);
                  return sameDepartment;
            }

            System.out.println("❌ User has no valid attendance viewing permissions");
            return false;
      }

      // Attendance düzenleme yetkisi kontrolü
      public boolean canEditAttendance(Authentication authentication, Long targetUserId) {
            String keycloakId = authentication.getName();
            User editor = userRepository.findByKeycloakId(keycloakId).orElse(null);
            if (editor == null) {
                  System.out.println("❌ Editor not found for keycloakId: " + keycloakId);
                  return false;
            }

            // Hedef kullanıcıyı bul
            User targetUser = userRepository.findById(targetUserId).orElse(null);
            if (targetUser == null) {
                  System.out.println("❌ Target user not found for userId: " + targetUserId);
                  return false;
            }

            // Kendi attendance'ını düzenleyebilir
            if (editor.getId().equals(targetUserId)) {
                  System.out.println("✅ User can edit their own attendance");
                  return true;
            }

            // Editor'ün yetkilerini kontrol et
            Role editorRole = editor.getRole();
            if (editorRole == null) {
                  System.out.println("❌ Editor has no role assigned");
                  return false;
            }

            List<RolePermission> rolePermissions = rolePermissionRepository.findByRoleId(editorRole.getId());
            List<String> userPermissions = rolePermissions.stream()
                  .map(rp -> rp.getPermission().getName())
                  .collect(Collectors.toList());

            System.out.println("🔍 Editor permissions: " + userPermissions);
            System.out.println("🔍 Editor department: " + (editor.getDepartment() != null ? editor.getDepartment().getName() : "null"));
            System.out.println("🔍 Target user department: " + (targetUser.getDepartment() != null ? targetUser.getDepartment().getName() : "null"));

            // ADMIN_ALL veya EDIT_ALL_ATTENDANCE yetkisi varsa direkt izin ver
            if (userPermissions.contains("ADMIN_ALL") || userPermissions.contains("EDIT_ALL_ATTENDANCE")) {
                  System.out.println("✅ User has ADMIN_ALL or EDIT_ALL_ATTENDANCE permission");
                  return true;
            }

            // EDIT_CHILD_ATTENDANCE yetkisi varsa, sadece child departmanlarındaki kullanıcıları düzenleyebilir
            if (userPermissions.contains("EDIT_CHILD_ATTENDANCE")) {
                  boolean isChild = isInChildDepartments(editor.getDepartment(), targetUser.getDepartment());
                  System.out.println("🔍 EDIT_CHILD_ATTENDANCE check: " + isChild);
                  return isChild;
            }

            // EDIT_DEPARTMENT_ATTENDANCE yetkisi varsa, sadece kendi departmanındaki kullanıcıları düzenleyebilir
            if (userPermissions.contains("EDIT_DEPARTMENT_ATTENDANCE")) {
                  boolean sameDepartment = editor.getDepartment().getId().equals(targetUser.getDepartment().getId());
                  System.out.println("🔍 EDIT_DEPARTMENT_ATTENDANCE check: " + sameDepartment);
                  return sameDepartment;
            }

            System.out.println("❌ User has no valid attendance editing permissions");
            return false;
      }

      // Bir departmanın child departmanlarında olup olmadığını kontrol eder
      private boolean isInChildDepartments(Department parentDepartment, Department childDepartment) {
            if (parentDepartment == null || childDepartment == null) {
                  return false;
            }

            // Aynı departman ise true
            if (parentDepartment.getId().equals(childDepartment.getId())) {
                  return true;
            }

            // DepartmentHierarchyService kullanarak child departmanları kontrol et
            try {
                  Set<Department> childDepartments = departmentHierarchyService.findAllDescendants(parentDepartment);
                  return childDepartments.contains(childDepartment);
            } catch (Exception e) {
                  // Hata durumunda false döndür
                  return false;
            }
      }
}
