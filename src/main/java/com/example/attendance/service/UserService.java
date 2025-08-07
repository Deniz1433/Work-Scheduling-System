package com.example.attendance.service;

import com.example.attendance.dto.UserDto;
import com.example.attendance.model.Department;
import com.example.attendance.model.Role;
import com.example.attendance.model.User;
import com.example.attendance.repository.DepartmentRepository;
import com.example.attendance.repository.RoleRepository;
import com.example.attendance.repository.UserRepository;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

      private final UserRepository userRepository;
      private final RoleRepository roleRepository;
      private final DepartmentRepository departmentRepository;
      private final Keycloak keycloakClient;

      @Value("${keycloak.realm}")
      private String realm;

      public List<UserDto> getAllUsers() {
            return userRepository.findAll().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
      }

      public Long getUserIdbyKeycloakId(String keycloakId) {
            Optional<User> optionalUser = userRepository.findByKeycloakId(keycloakId);
            if (optionalUser.isPresent()) {
                  return optionalUser.get().getId();
            }

            try {
                  // Keycloak'tan kullanıcıyı çek
                  UserRepresentation kcUser = keycloakClient
                          .realm(realm)
                          .users()
                          .get(keycloakId)
                          .toRepresentation();

                  User newUser = new User();
                  newUser.setKeycloakId(kcUser.getId());
                  newUser.setUsername(kcUser.getUsername());
                  newUser.setEmail(kcUser.getEmail());
                  newUser.setFirstName(kcUser.getFirstName());
                  newUser.setLastName(kcUser.getLastName());
                  newUser.setIsActive(true);

                  // default rol ve departman ata
                  Role defaultRole = roleRepository.findByName("staff")
                          .orElseThrow(() -> new RuntimeException("Default role 'staff' not found"));
                  Department defaultDept = departmentRepository.findByName("java")
                          .orElseThrow(() -> new RuntimeException("Default department 'default' not found"));

                  newUser.setRole(defaultRole);
                  newUser.setDepartment(defaultDept);

                  userRepository.save(newUser);
                  return newUser.getId();
            } catch (Exception e) {
                  log.error("Keycloak'tan kullanıcı çekilirken hata oluştu: {}", e.getMessage(), e);
                  throw new RuntimeException("Keycloak kullanıcı hatası: " + e.getMessage());
            }
      }

      public UserDto createUser(UserDto dto) {
            log.info("Yeni kullanıcı oluşturuluyor: {}", dto.getUsername());

            // Keycloak user oluştur
            UserRepresentation keycloakUser = new UserRepresentation();
            keycloakUser.setUsername(dto.getUsername());
            keycloakUser.setEmail(dto.getEmail());
            keycloakUser.setFirstName(dto.getFirstName());
            keycloakUser.setLastName(dto.getLastName());
            keycloakUser.setEnabled(true);

            Response response = keycloakClient.realm(realm).users().create(keycloakUser);
            String keycloakId = CreatedResponseUtil.getCreatedId(response);

            // Password ayarla
            CredentialRepresentation passwordRep = new CredentialRepresentation();
            passwordRep.setType(CredentialRepresentation.PASSWORD);
            passwordRep.setValue(dto.getPassword());
            passwordRep.setTemporary(false);
            keycloakClient.realm(realm).users().get(keycloakId).resetPassword(passwordRep);

            // PostgreSQL user oluştur
            User user = new User();
            user.setKeycloakId(keycloakId);
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            user.setFirstName(dto.getFirstName());
            user.setLastName(dto.getLastName());
            user.setPassword(dto.getPassword());
            user.setIsActive(true);

            if (dto.getRoleId() != null) {
                  Role role = roleRepository.findById(dto.getRoleId())
                          .orElseThrow(() -> new RuntimeException("Role bulunamadı: " + dto.getRoleId()));
                  user.setRole(role);
            }

            if (dto.getDepartmentId() != null) {
                  Department dept = departmentRepository.findById(dto.getDepartmentId())
                          .orElseThrow(() -> new RuntimeException("Departman bulunamadı: " + dto.getDepartmentId()));
                  user.setDepartment(dept);
            }

            userRepository.save(user);
            return toDto(user);
      }

      public void deleteUser(Long id) {
            userRepository.findById(id).ifPresent(user -> {
                  try {
                        keycloakClient.realm(realm).users().get(user.getKeycloakId()).remove();
                  } catch (Exception e) {
                        log.warn("Keycloak'tan silinemedi: {}", user.getKeycloakId());
                  }
                  userRepository.deleteById(id);
            });
      }

      public User getUserByKeycloakId(String keycloakId) {
            return userRepository.findByKeycloakId(keycloakId).orElse(null);
      }

      public User save(User user) {
            return userRepository.save(user);
      }

      private UserDto toDto(User user) {
            UserDto dto = new UserDto();
            BeanUtils.copyProperties(user, dto);
            if (user.getRole() != null) dto.setRoleId(user.getRole().getId());
            if (user.getDepartment() != null) dto.setDepartmentId(user.getDepartment().getId());
            return dto;
      }
      public UserDto getUserById(Long id) {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));

            return toDto(user);
      }

      // Tüm User entity listesini döndüren method
      public List<User> getAllUsersAsEntities() {
            return userRepository.findAll();
      }
}