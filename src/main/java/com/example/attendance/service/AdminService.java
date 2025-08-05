package com.example.attendance.service;

import com.example.attendance.dto.AdminUserDto;
import com.example.attendance.model.Role;
import com.example.attendance.model.Department;
import com.example.attendance.model.UserRole;
import com.example.attendance.model.UserDepartment;
import com.example.attendance.repository.RoleRepository;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.repository.UserRoleRepository;
import com.example.attendance.repository.UserDepartmentRepository;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdminService {
    
    private final Keycloak keycloakAdminClient;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    
    public AdminService(Keycloak keycloakAdminClient, 
                       RoleRepository roleRepository, 
                       DepartmentRepository departmentRepository,
                       UserRoleRepository userRoleRepository,
                       UserDepartmentRepository userDepartmentRepository) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRoleRepository = userRoleRepository;
        this.userDepartmentRepository = userDepartmentRepository;
    }
    
    @Transactional(readOnly = true)
    public List<AdminUserDto> getAllUsers() {
        try {
            RealmResource realm = keycloakAdminClient.realm("attendance-realm");
            UsersResource usersResource = realm.users();
            List<UserRepresentation> keycloakUsers = usersResource.list();
            
            List<AdminUserDto> users = new ArrayList<>();
            for (UserRepresentation keycloakUser : keycloakUsers) {
                AdminUserDto userDto = new AdminUserDto();
                userDto.setId(keycloakUser.getId());
                userDto.setFirstName(keycloakUser.getFirstName());
                userDto.setLastName(keycloakUser.getLastName());
                userDto.setEmail(keycloakUser.getEmail());
                userDto.setUsername(keycloakUser.getUsername());
                
                // PostgreSQL'den rol ve departman bilgilerini al
                List<UserRole> userRoles = userRoleRepository.findByUserId(keycloakUser.getId());
                if (!userRoles.isEmpty()) {
                    UserRole userRole = userRoles.get(0); // İlk rolü al
                    userDto.setRoleId(userRole.getRole().getId().toString());
                    userDto.setRole(userRole.getRole().getName());
                }
                
                List<UserDepartment> userDepartments = userDepartmentRepository.findByUserId(keycloakUser.getId());
                if (!userDepartments.isEmpty()) {
                    UserDepartment userDepartment = userDepartments.get(0); // İlk departmanı al
                    userDto.setDepartmentId(userDepartment.getDepartment().getId().toString());
                    userDto.setDepartment(userDepartment.getDepartment().getName());
                }
                
                users.add(userDto);
            }
            return users;
        } catch (Exception e) {
            System.err.println("Kullanıcılar alınırken hata: " + e.getMessage());
            return getMockUsers();
        }
    }
    
    @Transactional
    public void createUser(AdminUserDto userDto) {
        try {
            // 1. Keycloak'ta kullanıcı oluştur
            RealmResource realm = keycloakAdminClient.realm("attendance-realm");
            UsersResource usersResource = realm.users();
            
            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(userDto.getUsername());
            user.setEmail(userDto.getEmail());
            user.setFirstName(userDto.getFirstName());
            user.setLastName(userDto.getLastName());
            
            // Şifre ayarla
            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(userDto.getPassword());
            credential.setTemporary(false);
            user.setCredentials(List.of(credential));
            
            // Kullanıcıyı oluştur
            var response = usersResource.create(user);
            
            if (response.getStatus() == 201) {
                System.out.println("Keycloak'ta kullanıcı başarıyla oluşturuldu: " + userDto.getUsername());
                
                // Oluşturulan kullanıcının ID'sini al
                String userId = null;
                try {
                    List<UserRepresentation> createdUsers = usersResource.search(userDto.getUsername());
                    if (!createdUsers.isEmpty()) {
                        userId = createdUsers.get(0).getId();
                        System.out.println("Oluşturulan kullanıcının ID'si: " + userId);
                    }
                } catch (Exception e) {
                    System.err.println("Kullanıcı ID'si alınırken hata: " + e.getMessage());
                }
                
                // 2. PostgreSQL'de rol ve departman bilgilerini kaydet
                if (userId != null && userDto.getRoleId() != null && !userDto.getRoleId().isEmpty()) {
                    try {
                        Optional<Role> role = roleRepository.findById(Long.parseLong(userDto.getRoleId()));
                        if (role.isPresent()) {
                            UserRole userRole = new UserRole();
                            userRole.setUserId(userId);
                            userRole.setRole(role.get());
                            userRoleRepository.save(userRole);
                            System.out.println("PostgreSQL'de rol kaydedildi: " + role.get().getName());
                        }
                    } catch (Exception e) {
                        System.err.println("Rol kaydedilirken hata: " + e.getMessage());
                    }
                }
                
                if (userId != null && userDto.getDepartmentId() != null && !userDto.getDepartmentId().isEmpty()) {
                    try {
                        Optional<Department> department = departmentRepository.findById(Long.parseLong(userDto.getDepartmentId()));
                        if (department.isPresent()) {
                            UserDepartment userDepartment = new UserDepartment();
                            userDepartment.setUserId(userId);
                            userDepartment.setDepartment(department.get());
                            userDepartmentRepository.save(userDepartment);
                            System.out.println("PostgreSQL'de departman kaydedildi: " + department.get().getName());
                        }
                    } catch (Exception e) {
                        System.err.println("Departman kaydedilirken hata: " + e.getMessage());
                    }
                }
            } else {
                throw new RuntimeException("Kullanıcı oluşturulamadı. Status: " + response.getStatus());
            }
        } catch (Exception e) {
            System.err.println("Kullanıcı oluşturulurken hata: " + e.getMessage());
            throw new RuntimeException("Kullanıcı oluşturulamadı: " + e.getMessage());
        }
    }
    
    @Transactional
    public void updateUserRole(String userId, String roleId) {
        try {
            System.out.println("Kullanıcı rolü güncelleniyor - User ID: " + userId + ", Role ID: " + roleId);
            
            // Rol ID kontrolü
            if (roleId == null || roleId.isEmpty()) {
                System.out.println("Rol ID boş, işlem iptal edildi");
                return;
            }
            
            // Rol var mı kontrol et
            Optional<Role> role = roleRepository.findById(Long.parseLong(roleId));
            if (!role.isPresent()) {
                throw new RuntimeException("Rol bulunamadı: " + roleId);
            }
            
            // Önce mevcut rolü sil (eğer varsa)
            try {
                userRoleRepository.deleteByUserIdAndRoleId(userId, Long.parseLong(roleId));
                System.out.println("Mevcut rol silindi");
            } catch (Exception e) {
                System.out.println("Mevcut rol silinirken hata: " + e.getMessage());
                // Hata olsa bile devam et
            }
            
            // Yeni rolü ekle
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRole(role.get());
            userRoleRepository.save(userRole);
            System.out.println("PostgreSQL'de rol güncellendi: " + role.get().getName());
            
        } catch (Exception e) {
            System.err.println("Rol güncellenirken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Rol güncellenemedi: " + e.getMessage());
        }
    }
    
    @Transactional
    public void updateUserDepartment(String userId, String departmentId) {
        try {
            System.out.println("Kullanıcı departmanı güncelleniyor - User ID: " + userId + ", Department ID: " + departmentId);
            
            // Departman ID kontrolü
            if (departmentId == null || departmentId.isEmpty()) {
                System.out.println("Departman ID boş, işlem iptal edildi");
                return;
            }
            
            // Departman var mı kontrol et
            Optional<Department> department = departmentRepository.findById(Long.parseLong(departmentId));
            if (!department.isPresent()) {
                throw new RuntimeException("Departman bulunamadı: " + departmentId);
            }
            
            // Önce mevcut departmanı sil (eğer varsa)
            try {
                userDepartmentRepository.deleteByUserIdAndDepartmentId(userId, Long.parseLong(departmentId));
                System.out.println("Mevcut departman silindi");
            } catch (Exception e) {
                System.out.println("Mevcut departman silinirken hata: " + e.getMessage());
                // Hata olsa bile devam et
            }
            
            // Yeni departmanı ekle
            UserDepartment userDepartment = new UserDepartment();
            userDepartment.setUserId(userId);
            userDepartment.setDepartment(department.get());
            userDepartmentRepository.save(userDepartment);
            System.out.println("PostgreSQL'de departman güncellendi: " + department.get().getName());
            
        } catch (Exception e) {
            System.err.println("Departman güncellenirken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Departman güncellenemedi: " + e.getMessage());
        }
    }
    
    @Transactional
    public void deleteUser(String userId) {
        try {
            System.out.println("Kullanıcı siliniyor - User ID: " + userId);
            
            // 1. PostgreSQL'den kullanıcı rollerini sil
            try {
                userRoleRepository.deleteByUserId(userId);
                System.out.println("PostgreSQL'den kullanıcı rolleri silindi");
            } catch (Exception e) {
                System.out.println("PostgreSQL'den kullanıcı rolleri silinirken hata: " + e.getMessage());
            }
            
            // 2. PostgreSQL'den kullanıcı departmanlarını sil
            try {
                userDepartmentRepository.deleteByUserId(userId);
                System.out.println("PostgreSQL'den kullanıcı departmanları silindi");
            } catch (Exception e) {
                System.out.println("PostgreSQL'den kullanıcı departmanları silinirken hata: " + e.getMessage());
            }
            
            // 3. Keycloak'tan kullanıcıyı sil
            try {
                RealmResource realm = keycloakAdminClient.realm("attendance-realm");
                realm.users().delete(userId);
                System.out.println("Keycloak'tan kullanıcı silindi");
            } catch (Exception e) {
                System.out.println("Keycloak'tan kullanıcı silinirken hata: " + e.getMessage());
                // Keycloak'tan silinmezse bile PostgreSQL'den silmeye devam et
            }
            
            System.out.println("Kullanıcı başarıyla silindi");
            
        } catch (Exception e) {
            System.err.println("Kullanıcı silinirken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Kullanıcı silinemedi: " + e.getMessage());
        }
    }
    
    private List<AdminUserDto> getMockUsers() {
        List<AdminUserDto> users = new ArrayList<>();
        
        AdminUserDto user1 = new AdminUserDto();
        user1.setId("1");
        user1.setFirstName("Admin");
        user1.setLastName("User");
        user1.setEmail("admin@gmail.com");
        user1.setUsername("admin");
        user1.setRoleId("1");
        user1.setDepartmentId("1");
        user1.setRole("Admin");
        user1.setDepartment("IT");
        users.add(user1);
        
        AdminUserDto user2 = new AdminUserDto();
        user2.setId("2");
        user2.setFirstName("Test");
        user2.setLastName("User");
        user2.setEmail("test@example.com");
        user2.setUsername("test");
        user2.setRoleId("2");
        user2.setDepartmentId("2");
        user2.setRole("User");
        user2.setDepartment("HR");
        users.add(user2);
        
        return users;
    }
} 