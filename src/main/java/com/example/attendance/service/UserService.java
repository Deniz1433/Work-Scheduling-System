// UserService.java
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
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
            return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
      }

      public UserDto getUserById(UUID id) {
            User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
            return toDto(user);
      }

      public UserDto createUser(UserDto dto) {
            // 1. Create user in Keycloak
            UserRepresentation keycloakUser = new UserRepresentation();
            keycloakUser.setUsername(dto.getUsername());
            keycloakUser.setEmail(dto.getEmail());
            keycloakUser.setFirstName(dto.getFirstName());
            keycloakUser.setLastName(dto.getLastName());
            keycloakUser.setEnabled(true);

            Response response = keycloakClient.realm(realm).users().create(keycloakUser);
            String keycloakId = CreatedResponseUtil.getCreatedId(response);

            // 2. Set password
            CredentialRepresentation passwordRep = new CredentialRepresentation();
            passwordRep.setType(CredentialRepresentation.PASSWORD);
            passwordRep.setValue(dto.getPassword());
            passwordRep.setTemporary(false);
            keycloakClient.realm(realm).users().get(keycloakId).resetPassword(passwordRep);

            // 3. Mirror to PostgreSQL
            User user = new User();
            user.setId(UUID.randomUUID());
            user.setKeycloakId(keycloakId);
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            user.setFirstName(dto.getFirstName());
            user.setLastName(dto.getLastName());
            user.setPassword(dto.getPassword());
            user.setIsActive(true);

            if (dto.getRoleId() != null) {
                  Role role = (Role) roleRepository.findById(dto.getRoleId()).orElseThrow(() -> new RuntimeException("Role not found"));
                  user.setRole(role);
            }

            if (dto.getDepartmentId() != null) {
                  Department dept = (Department) departmentRepository.findById(dto.getDepartmentId()).orElseThrow(() -> new RuntimeException("Department not found"));
                  user.setDepartment(dept);
            }

            userRepository.save(user);
            return toDto(user);
      }

      public void deleteUser(UUID id) {
            Optional<User> optUser = userRepository.findById(id);
            optUser.ifPresent(user -> {
                  try {
                        keycloakClient.realm(realm).users().get(user.getKeycloakId()).remove();
                  } catch (Exception ignored) {}
                  userRepository.deleteById(id);
            });
      }

      private UserDto toDto(User user) {
            UserDto dto = new UserDto();
            BeanUtils.copyProperties(user, dto);
            if (user.getRole() != null) dto.setRoleId(user.getRole().getId());
            if (user.getDepartment() != null) dto.setDepartmentId(user.getDepartment().getId());
            return dto;
      }
}