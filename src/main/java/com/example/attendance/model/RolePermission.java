package com.example.attendance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "role_permission")
public class RolePermission {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;
      
      @ManyToOne
      @JoinColumn(name = "role_id", nullable = false)
      private Role role;
      
      @ManyToOne
      @JoinColumn(name = "permission_id", nullable = false)
      private Permission permission;
}
