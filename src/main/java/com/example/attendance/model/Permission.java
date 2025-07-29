package com.example.attendance.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "permission")
public class Permission {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;
      @Column(name = "name", nullable = false, length = 36)
      private String name;
      @Column(name = "description", nullable = false, length = 36)
      private String description;
}
