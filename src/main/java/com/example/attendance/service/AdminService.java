package com.example.attendance.service;

import com.example.attendance.dto.CreateUserDto;
import com.example.attendance.dto.UserDto;
import com.example.attendance.model.Role;
import com.example.attendance.model.User;
import com.example.attendance.model.Department;
import com.example.attendance.repository.RoleRepository;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.repository.UserRepository;

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
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final KeycloakAdminService keycloakAdminService;

    public AdminService(Keycloak keycloakAdminClient,
                        RoleRepository roleRepository,
                        DepartmentRepository departmentRepository,
                        UserRepository userRepository, KeycloakAdminService keycloakAdminService) {
        this.keycloakAdminClient = keycloakAdminClient;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.keycloakAdminService = keycloakAdminService;
    }

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        try {
            // Sadece PostgreSQL'de olan kullanıcıları al
            List<User> dbUsers = userRepository.findAll();
            List<UserDto> users = new ArrayList<>();
            
            for (User dbUser : dbUsers) {
                UserDto userDto = new UserDto();
                userDto.setId(dbUser.getId());
                userDto.setKeycloakId(dbUser.getKeycloakId());
                userDto.setFirstName(dbUser.getFirstName());
                userDto.setLastName(dbUser.getLastName());
                userDto.setEmail(dbUser.getEmail());
                userDto.setUsername(dbUser.getUsername());
                userDto.setDepartmentId(dbUser.getDepartment() != null ? dbUser.getDepartment().getId() : null);
                userDto.setRoleId(dbUser.getRole() != null ? dbUser.getRole().getId() : null);
                
                users.add(userDto);
            }
            
            return users;
        } catch (Exception e) {
            System.err.println("Kullanıcılar alınırken hata: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return null;
            }

            UserDto userDto = new UserDto();
            userDto.setId(user.getId());
            userDto.setKeycloakId(user.getKeycloakId());
            userDto.setFirstName(user.getFirstName());
            userDto.setLastName(user.getLastName());
            userDto.setEmail(user.getEmail());
            userDto.setUsername(user.getUsername());
            userDto.setDepartmentId(user.getDepartment() != null ? user.getDepartment().getId() : null);
            userDto.setRoleId(user.getRole() != null ? user.getRole().getId() : null);

            return userDto;
        } catch (Exception e) {
            System.err.println("Kullanıcı alınırken hata: " + e.getMessage());
            return null;
        }
    }

    @Transactional
    public UserRepresentation createKeycloakUser(UserDto userDto) {
        try {
            RealmResource realm = keycloakAdminClient.realm("attendance-realm");
            UsersResource usersResource = realm.users();

            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(userDto.getUsername());
            user.setEmail(userDto.getEmail());
            user.setFirstName(userDto.getFirstName());
            user.setLastName(userDto.getLastName());

            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(userDto.getPassword());
            credential.setTemporary(false);
            user.setCredentials(List.of(credential));

            var response = usersResource.create(user);

            if (response.getStatus() == 201) {
                System.out.println("Keycloak'ta kullanıcı başarıyla oluşturuldu: " + userDto.getUsername());

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
            } else {
                throw new RuntimeException("Kullanıcı oluşturulamadı. Status: " + response.getStatus());
            }

            return user;
        } catch (Exception e) {
            System.err.println("Keycloak kullanıcısı oluşturulurken hata: " + e.getMessage());
            throw new RuntimeException("Kullanıcı oluşturulamadı: " + e.getMessage());
        }
    }

    @Transactional
    public void createUser(UserDto userDto, String keycloakId) {
        try {
            if (userDto.getDepartmentId() == null) {
                throw new IllegalArgumentException("Departman seçilmedi");
            }
            User user = new User();
            user.setIsActive(true);
            user.setKeycloakId(keycloakId);
            user.setUsername(userDto.getUsername());
            user.setEmail(userDto.getEmail());
            user.setFirstName(userDto.getFirstName());
            user.setLastName(userDto.getLastName());
            user.setPassword(userDto.getPassword());
            user.setDepartment(departmentRepository.findById(userDto.getDepartmentId()).orElse(null));
            user.setRole(roleRepository.findById(userDto.getRoleId()).orElse(null));

            userRepository.save(user);  // ✅ PostgreSQL'e kaydet

            System.out.println("PostgreSQL'de kullanıcı başarıyla oluşturuldu: " + user.getUsername());

        } catch (Exception e) {
            System.err.println("Kullanıcı oluşturulurken hata: " + e.getMessage());
            throw new RuntimeException("Kullanıcı oluşturulamadı: " + e.getMessage());
        }
    }
    public void createUser(CreateUserDto dto) {
        // 1. Keycloak’ta kullanıcıyı oluştur
        String keycloakId = keycloakAdminService.createKeycloakUser(dto);

        // 2. PostgreSQL’e ekle (ŞU ANDA BU KISIM YOK YA DA ÇALIŞMIYOR)
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setIsActive(true);
        user.setPassword(dto.getPassword()); // çünkü Keycloak’ta tutuluyor
        System.out.println("Gelen Role ID: " + dto.getRoleId());
        user.setRole(roleRepository.findById(dto.getRoleId()).orElseThrow());
        System.out.println("Gelen departmentId: " + dto.getDepartmentId());
        user.setDepartment(departmentRepository.findById(dto.getDepartmentId()).orElseThrow());

        userRepository.save(user); // ← BU SATIR PG’ye ekler
    }

    @Transactional
    public void updateUserRole(Long userId, Long roleId) {
        try {
            System.out.println("Kullanıcı rolü güncelleniyor - User ID: " + userId + ", Role ID: " + roleId);

            if (roleId == null) {
                System.out.println("Rol ID boş, işlem iptal edildi");
                return;
            }

            User u = userRepository.findById(userId).orElse(null);
            if (u == null) {
                System.out.println("Kullanıcı bulunamadı");
                throw new RuntimeException("Kullanıcı bulunamadı");
            } else {
                Role role = roleRepository.findById(roleId).orElse(null);
                if (role == null) {
                    System.out.println("Rol bulunamadı");
                    throw new RuntimeException("Rol bulunamadı");
                }
                u.setRole(role);
                userRepository.save(u); // Değişiklikleri kaydet
                System.out.println("Kullanıcı rolü başarıyla güncellendi");
            }
        } catch (Exception e) {
            System.err.println("Rol güncellenirken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Rol güncellenemedi: " + e.getMessage());
        }
    }

    @Transactional
    public void updateUserDepartment(Long userId, Long departmentId) {
        try {
            System.out.println("Kullanıcı departmanı güncelleniyor - User ID: " + userId + ", Department ID: " + departmentId);

            if (departmentId == null) {
                System.out.println("Departman ID boş, işlem iptal edildi");
                return;
            }

            User u = userRepository.findById(userId).orElse(null);
            if (u == null) {
                System.out.println("Kullanıcı bulunamadı");
                throw new RuntimeException("Kullanıcı bulunamadı");
            } else {
                Department department = departmentRepository.findById(departmentId).orElse(null);
                if (department == null) {
                    System.out.println("Departman bulunamadı");
                    throw new RuntimeException("Departman bulunamadı");
                }
                u.setDepartment(department);
                userRepository.save(u); // Değişiklikleri kaydet
                System.out.println("Kullanıcı departmanı başarıyla güncellendi");
            }
        } catch (Exception e) {
            System.err.println("Departman güncellenirken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Departman güncellenemedi: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteUser(String keycloakId) {
        try {
            System.out.println("Kullanıcı siliniyor - Keycloak ID: " + keycloakId);

            // Önce PostgreSQL'den sil
            try {
                User u = userRepository.findByKeycloakId(keycloakId).orElse(null);
                if (u != null) {
                    userRepository.deleteByKeycloakId(keycloakId);
                    System.out.println("PostgreSQL'den kullanıcı silindi");
                } else {
                    System.out.println("PostgreSQL'de kullanıcı bulunamadı");
                }
            } catch (Exception e) {
                System.out.println("PostgreSQL'den kullanıcı silinirken hata: " + e.getMessage());
            }

            // Sonra Keycloak'tan sil
            try {
                RealmResource realm = keycloakAdminClient.realm("attendance-realm");
                realm.users().delete(keycloakId);
                System.out.println("Keycloak'tan kullanıcı silindi");
            } catch (Exception e) {
                System.out.println("Keycloak'tan kullanıcı silinirken hata: " + e.getMessage());
                // Keycloak'tan silinmezse bile PostgreSQL'den silindiği için devam et
            }

        } catch (Exception e) {
            System.err.println("Kullanıcı silinirken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Kullanıcı silinemedi: " + e.getMessage());
        }
    }

    @Transactional
    public void syncUsersFromKeycloak() {
        try {
            System.out.println("Keycloak'tan kullanıcılar senkronize ediliyor...");
            
            RealmResource realm = keycloakAdminClient.realm("attendance-realm");
            UsersResource usersResource = realm.users();
            List<UserRepresentation> keycloakUsers = usersResource.list();

            int addedCount = 0;
            int updatedCount = 0;

            for (UserRepresentation keycloakUser : keycloakUsers) {
                // PostgreSQL'de bu kullanıcı var mı kontrol et
                Optional<User> existingUser = userRepository.findByKeycloakId(keycloakUser.getId());
                
                if (existingUser.isPresent()) {
                    // Kullanıcı zaten var, güncelle
                    User user = existingUser.get();
                    user.setFirstName(keycloakUser.getFirstName());
                    user.setLastName(keycloakUser.getLastName());
                    user.setEmail(keycloakUser.getEmail());
                    user.setUsername(keycloakUser.getUsername());
                    userRepository.save(user);
                    updatedCount++;
                    System.out.println("Kullanıcı güncellendi: " + keycloakUser.getUsername());
                } else {
                    // Yeni kullanıcı, ekle
                    User newUser = new User();
                    newUser.setKeycloakId(keycloakUser.getId());
                    newUser.setFirstName(keycloakUser.getFirstName());
                    newUser.setLastName(keycloakUser.getLastName());
                    newUser.setEmail(keycloakUser.getEmail());
                    newUser.setUsername(keycloakUser.getUsername());
                    newUser.setIsActive(true);
                    newUser.setPassword(""); // Keycloak'ta tutuluyor
                    
                    // Varsayılan rol ve departman ata (ID'leri kontrol et)
                    try {
                        Role defaultRole = roleRepository.findById(1L).orElse(null);
                        Department defaultDept = departmentRepository.findById(1L).orElse(null);
                        
                        newUser.setRole(defaultRole);
                        newUser.setDepartment(defaultDept);
                        
                        userRepository.save(newUser);
                        addedCount++;
                        System.out.println("Yeni kullanıcı eklendi: " + keycloakUser.getUsername());
                    } catch (Exception e) {
                        System.err.println("Kullanıcı eklenirken hata: " + keycloakUser.getUsername() + " - " + e.getMessage());
                    }
                }
            }
            
            System.out.println("Senkronizasyon tamamlandı. Eklenen: " + addedCount + ", Güncellenen: " + updatedCount);
            
        } catch (Exception e) {
            System.err.println("Senkronizasyon hatası: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Kullanıcılar senkronize edilemedi: " + e.getMessage());
        }
    }
}