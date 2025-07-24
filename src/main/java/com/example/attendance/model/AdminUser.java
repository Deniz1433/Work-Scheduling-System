package com.example.attendance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "admin_user")
public class AdminUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "name", nullable = false, length = 30)
    private String name;
    @Column(name = "surname", nullable = false, length = 30)
    private String surname;
    @Column(name = "email", nullable = true, length = 36)
    private String email;
    @Column(name = "username", nullable = false, length = 15)
    private String username;
    @Column(name = "password", nullable = true, length = 36)
    private String password;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

}
