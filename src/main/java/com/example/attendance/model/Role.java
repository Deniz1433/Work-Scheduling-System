package com.example.attendance.model;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "role")
public class Role{
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;
      @Column(name = "name", nullable = false, length = 36)
      private String name;
      @Column(name = "description", nullable = true, length = 36)
      private String description;
      @Column(name = "is_active", nullable = false)
      private Boolean isActive = true;

}