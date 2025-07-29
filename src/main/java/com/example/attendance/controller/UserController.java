package com.example.attendance.controller;

import com.example.attendance.dto.UserDto;
import com.example.attendance.model.Permission;
import com.example.attendance.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/admin/{userId}")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public List<UserDto> listAllUsers(@PathVariable String userId) {
        UserDto user = userService.getUserById(userId);
        List<Permission> permissions = userService.getPermissions(user.getId());
        boolean isAdmin = permissions.stream()
                .anyMatch(p -> p.getId() == 1 || p.getId() == 2);
        if(isAdmin) {
            return userService.getAllUsers();
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to access this resource");
        }
    }


}
