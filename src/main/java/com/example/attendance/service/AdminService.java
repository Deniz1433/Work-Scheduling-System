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
            RealmResource realm = keycloakAdminClient.realm("attendance-realm");
            UsersResource usersResource = realm.users();
            List<UserRepresentation> keycloakUsers = usersResource.list();

            List<UserDto> users = new ArrayList<>();
            for (UserRepresentation keycloakUser : keycloakUsers) {
                UserDto userDto = new UserDto();
                userDto.setKeycloakId(keycloakUser.getId());
                userDto.setFirstName(keycloakUser.getFirstName());
                userDto.setLastName(keycloakUser.getLastName());
                userDto.setEmail(keycloakUser.getEmail());
                userDto.setUsername(keycloakUser.getUsername());

                User u = userRepository.findByKeycloakId(keycloakUser.getId()).orElse(null);
                if (u != null) {
                    userDto.setId(u.getId()); // <- Bu satır kritik
                    userDto.setDepartmentId(u.getDepartment().getId());
                    userDto.setRoleId(u.getRole().getId());
                } else {
                    userDto.setDepartmentId(null);
                    userDto.setRoleId(null);
                }

                users.add(userDto);
            }
            return users;
        } catch (Exception e) {
            System.err.println("Kullanıcılar alınırken hata: " + e.getMessage());
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
            } else {
                u.setRole(roleRepository.findById(roleId).orElse(null));
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
            } else {
                u.setDepartment(departmentRepository.findById(departmentId).orElse(null));
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

            try {
                RealmResource realm = keycloakAdminClient.realm("attendance-realm");
                realm.users().delete(keycloakId);
                System.out.println("Keycloak'tan kullanıcı silindi");
            } catch (Exception e) {
                System.out.println("Keycloak'tan kullanıcı silinirken hata: " + e.getMessage());
            }

            try {
                User u = userRepository.findByKeycloakId(keycloakId).orElse(null);
                if (u == null) {
                    System.out.println("Kullanıcı bulunamadı");
                } else {
                    userRepository.deleteByKeycloakId(keycloakId);
                }
            } catch (Exception e) {
                System.out.println("Users tablosundan kullanıcı silinirken hata: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Kullanıcı silinirken hata: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Kullanıcı silinemedi: " + e.getMessage());
        }
    }
}