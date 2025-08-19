package com.example.attendance.service;

import com.example.attendance.dto.CreateUserDto;
import com.example.attendance.dto.UserDto;
import com.example.attendance.model.Department;
import com.example.attendance.model.Role;
import com.example.attendance.model.User;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.repository.RoleRepository;
import com.example.attendance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final Keycloak keycloakAdminClient;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final KeycloakAdminService keycloakAdminService;

    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        try {
            List<UserDto> users = new ArrayList<>();
            for (User dbUser : userRepository.findAll()) {
                users.add(toDto(dbUser));
            }
            return users;
        } catch (Exception e) {
            log.error("Error fetching users: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(this::toDto)
                .orElse(null);
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
            if (response.getStatus() != 201) {
                throw new RuntimeException("Failed to create user in Keycloak. Status: " + response.getStatus());
            }

            return user;
        } catch (Exception e) {
            log.error("Error creating Keycloak user: {}", e.getMessage(), e);
            throw new RuntimeException("Keycloak user creation failed: " + e.getMessage());
        }
    }

    @Transactional
    public void createUser(UserDto userDto, String keycloakId) {
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
        userRepository.save(user);
        log.info("User created in Postgres: {}", user.getUsername());
    }

    @Transactional
    public void updateUserRole(Long userId, Long roleId) {
        User u = userRepository.findById(userId).orElseThrow();
        u.setRole(roleRepository.findById(roleId).orElseThrow());
        userRepository.save(u);
    }

    @Transactional
    public void updateUserDepartment(Long userId, Long departmentId) {
        User u = userRepository.findById(userId).orElseThrow();
        u.setDepartment(departmentRepository.findById(departmentId).orElseThrow());
        userRepository.save(u);
    }

    @Transactional
    public void deleteUser(String keycloakId) {
        userRepository.deleteByKeycloakId(keycloakId);
        try {
            keycloakAdminClient.realm("attendance-realm").users().delete(keycloakId);
        } catch (Exception e) {
            log.warn("Error deleting from Keycloak: {}", e.getMessage());
        }
    }

    public void syncUsersFromKeycloak() {
        try {
            RealmResource realm = keycloakAdminClient.realm("attendance-realm");
            List<UserRepresentation> keycloakUsers = realm.users().list();

            Role defaultRole = getOrCreateDefaultRole();
            Department defaultDept = getOrCreateDefaultDepartment();

            for (UserRepresentation kcUser : keycloakUsers) {
                try {
                    upsertOneKeycloakUser(kcUser, defaultRole, defaultDept);
                } catch (Exception e) {
                    log.error("Failed to sync {}: {}", kcUser.getUsername(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Sync error: {}", e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertOneKeycloakUser(UserRepresentation kc, Role defaultRole, Department defaultDept) {
        Optional<User> existing = userRepository.findByKeycloakId(kc.getId());
        if (existing.isPresent()) {
            User u = existing.get();
            u.setFirstName(kc.getFirstName());
            u.setLastName(kc.getLastName());
            u.setEmail(kc.getEmail());
            u.setUsername(kc.getUsername());
            userRepository.save(u);
        } else {
            User u = new User();
            u.setKeycloakId(kc.getId());
            u.setFirstName(kc.getFirstName());
            u.setLastName(kc.getLastName());
            u.setEmail(kc.getEmail());
            u.setUsername(kc.getUsername());
            u.setIsActive(true);
            u.setPassword("");
            u.setRole(defaultRole);
            u.setDepartment(defaultDept);
            userRepository.save(u);
        }
    }

    private Role getOrCreateDefaultRole() {
        return roleRepository.findByName("USER").orElseGet(() -> {
            Role r = new Role();
            r.setName("USER");
            r.setDescription("Default user role");
            r.setIsActive(true);
            return roleRepository.save(r);
        });
    }

    private Department getOrCreateDefaultDepartment() {
        return departmentRepository.findByName("Default").orElseGet(() -> {
            Department d = new Department();
            d.setName("Default");
            d.setMinDays(0);
            return departmentRepository.save(d);
        });
    }

    private UserDto toDto(User dbUser) {
        UserDto dto = new UserDto();
        dto.setId(dbUser.getId());
        dto.setKeycloakId(dbUser.getKeycloakId());
        dto.setFirstName(dbUser.getFirstName());
        dto.setLastName(dbUser.getLastName());
        dto.setEmail(dbUser.getEmail());
        dto.setUsername(dbUser.getUsername());
        dto.setDepartmentId(dbUser.getDepartment() != null ? dbUser.getDepartment().getId() : null);
        dto.setRoleId(dbUser.getRole() != null ? dbUser.getRole().getId() : null);
        return dto;
    }
    public UserDto createUser(CreateUserDto dto) {
        // 1. Veritabanında var mı kontrolü
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Bu kullanıcı adı zaten kullanılıyor.");
        }

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Bu e-posta zaten kullanılıyor.");
        }

        // 2. Keycloak'ta oluştur
        String keycloakId = keycloakAdminService.createKeycloakUser(dto);

        // 3. PostgreSQL'e kaydet
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setIsActive(true);
        user.setPassword(dto.getPassword());
        user.setRole(roleRepository.findById(dto.getRoleId()).orElseThrow(() -> new IllegalArgumentException("Geçersiz rol ID")));
        user.setDepartment(departmentRepository.findById(dto.getDepartmentId()).orElseThrow(() -> new IllegalArgumentException("Geçersiz departman ID")));

        userRepository.save(user);
        return toDto(user); // geri dön
    }
}
