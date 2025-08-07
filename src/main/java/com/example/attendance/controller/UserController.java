package com.example.attendance.controller;

import com.example.attendance.model.User;
import com.example.attendance.service.UserService;
import com.example.attendance.model.Role;
import com.example.attendance.model.Department;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/userInfo")
@CrossOrigin(origins = "*")
public class UserController {

      private final UserService userService;

      public UserController(UserService userService) {
            this.userService = userService;
      }

      // Eğer kullanıcı yoksa veritabanına kaydet
      @GetMapping("/{keycloakId}")
      public ResponseEntity<Long> getOrCreateUserIdByKeycloakId(@PathVariable String keycloakId) {
            Long userId = userService.getUserIdbyKeycloakId(keycloakId);

            if (userId != null) {
                  return ResponseEntity.ok(userId);
            }

            // Keycloak'tan authentication context üzerinden username/email alıyoruz
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            // Örnek default user
            User newUser = new User();
            newUser.setKeycloakId(keycloakId);
            newUser.setUsername(username);
            newUser.setEmail(username + "@auto.com"); // ya da JWT içinden al
            newUser.setFirstName("Auto");
            newUser.setLastName("Created");
            newUser.setIsActive(true);

            // Varsayılan rol ve departman veriyoruz (id = 1 olarak varsaydık)
            Role defaultRole = new Role();
            defaultRole.setId(1L);
            newUser.setRole(defaultRole);

            Department defaultDepartment = new Department();
            defaultDepartment.setId(1L);
            newUser.setDepartment(defaultDepartment);

            User savedUser = userService.save(newUser);
            return ResponseEntity.ok(savedUser.getId());
      }

      @GetMapping("")
      public ResponseEntity<Long> getCurrentUserId() {
            String keycloakId = SecurityContextHolder.getContext().getAuthentication().getName();
            return getOrCreateUserIdByKeycloakId(keycloakId);
      }

      @GetMapping("/current")
      public ResponseEntity<User> getCurrentUser() {
            String keycloakId = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.getUserByKeycloakId(keycloakId);
            if (user != null) {
                  return ResponseEntity.ok(user);
            }
            return ResponseEntity.notFound().build();
      }
}