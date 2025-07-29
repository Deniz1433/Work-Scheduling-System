package com.example.attendance.service;

import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import com.example.attendance.repository.RolePermissionRepository;
import com.example.attendance.repository.UserRepository;
import com.example.attendance.dto.UserDto;
import com.example.attendance.model.Permission;
import com.example.attendance.model.User;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
      private final UserRepository repo;
      private final KeycloakAdminService kcService;
      private final RolePermissionRepository rolePermissionRepo;

      public UserService(UserRepository repo, KeycloakAdminService kcService, RolePermissionRepository rolePermissionRepo) {
            this.repo = repo;
            this.kcService = kcService;
            this.rolePermissionRepo = rolePermissionRepo;
      }

      public List<UserDto> getAllUsers() {
            return repo.findAll().stream()
                        .map(UserDto::new)
                        .collect(Collectors.toList());
      }

      public UserDto getUserById(String id) {
            User user = repo.findById(id).orElseThrow(
                  () -> new RuntimeException("User not found"));
            return new UserDto(user);
      }

      public List<UserDto> getUsersFromKeycloak() {
            List<UserRepresentation> keycloakUsers = kcService.getAllUsers();
            return keycloakUsers.stream()
                        .map(userRepresentation -> new UserDto(userRepresentation))
                        .collect(Collectors.toList());
      }

      public void addUser(UserDto userDto) {
            kcService.addUser(userDto);
      }

      public void updateUser(UserDto userDto) {
            User user = repo.findById(userDto.getId()).orElseThrow(
                  () -> new RuntimeException("User not found"));
            user.setFirstName(userDto.getFirstName());
            user.setLastName(userDto.getLastName());
            user.setEmail(userDto.getEmail());
            user.setUsername(userDto.getUsername());
      }

      public List<Permission> getPermissions(String userId) {
            UserDto userDto = getUserById(userId);
            List<Permission> Permissions = rolePermissionRepo.findByRoleId(userDto.getRoleId()).stream()
                  .map(p -> p.getPermission())
                  .collect(Collectors.toList());
            return Permissions;
      }
}
