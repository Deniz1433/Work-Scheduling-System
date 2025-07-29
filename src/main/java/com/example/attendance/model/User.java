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
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "first_name", nullable = true, length = 30)
    private String firstName;
    @Column(name = "last_name", nullable = true, length = 30)
    private String lastName;
    @Column(name = "email", nullable = true, length = 36)
    private String email;
    @Column(name = "username", nullable = false, length = 15)
    private String username;
    @Column(name = "password", nullable = true, length = 36)
    private String password;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

}
