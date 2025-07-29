package com.example.attendance.dto;

import org.keycloak.representations.idm.UserRepresentation;

import com.example.attendance.model.Department;
import com.example.attendance.model.Role;
import com.example.attendance.model.User;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String password;
    private Long roleId;
    private Long departmentId;
     

    public UserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.roleId = user.getRole().getId();
        this.departmentId = user.getDepartment().getId();
    }

    public UserDto(UserRepresentation userRepresentation, Role role, Department department) {
        this.id = userRepresentation.getId();
        this.username = userRepresentation.getUsername();
        this.email = userRepresentation.getEmail();
        this.password = userRepresentation.getCredentials().isEmpty() ? null : userRepresentation.getCredentials().get(0).getValue();
        this.firstName = userRepresentation.getFirstName();
        this.lastName = userRepresentation.getLastName();
        this.roleId = role.getId();
        this.departmentId = department.getId();
    }
    public UserDto(UserRepresentation userRepresentation) {
        this.id = userRepresentation.getId();
        this.username = userRepresentation.getUsername();
        this.email = userRepresentation.getEmail();
        this.password = userRepresentation.getCredentials().isEmpty() ? null : userRepresentation.getCredentials().get(0).getValue();
        this.firstName = userRepresentation.getFirstName();
        this.lastName = userRepresentation.getLastName();
        this.roleId = null;
        this.departmentId = null;
    }
    public UserDto(UserRepresentation userRepresentation, Role role) {
        this.id = userRepresentation.getId();
        this.username = userRepresentation.getUsername();
        this.email = userRepresentation.getEmail();
        this.password = userRepresentation.getCredentials().isEmpty() ? null : userRepresentation.getCredentials().get(0).getValue();
        this.firstName = userRepresentation.getFirstName();
        this.lastName = userRepresentation.getLastName();
        this.roleId = role.getId();
        this.departmentId = null;
    }
    public UserDto(UserRepresentation userRepresentation, Department department) {
        this.id = userRepresentation.getId();
        this.username = userRepresentation.getUsername();
        this.email = userRepresentation.getEmail();
        this.password = userRepresentation.getCredentials().isEmpty() ? null : userRepresentation.getCredentials().get(0).getValue();
        this.firstName = userRepresentation.getFirstName();
        this.lastName = userRepresentation.getLastName();
        this.roleId = null;
        this.departmentId = department.getId();
    }
}
