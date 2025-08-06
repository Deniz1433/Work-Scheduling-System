package com.example.attendance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendance.service.UserService;

@RestController
@RequestMapping("/api/userInfo")
@CrossOrigin
public class UserController {
      private final UserService userService;

      public UserController(UserService userService) {
            this.userService = userService;
      }


      @GetMapping("/{keycloakId}")
      public ResponseEntity<Long> getUserIdByKeycloakId(@PathVariable String keycloakId) {
            Long userId = userService.getUserIdbyKeycloakId(keycloakId);
            if (userId == null) {
                  return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(userId);
      }
}
