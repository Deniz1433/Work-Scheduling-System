package com.example.attendance.model;

import jakarta.persistence.*;

public class Condition {
      @Id
      private Long id;
      @Column(name = "name", nullable = false, length = 36)
      private String name;
      @Column(name = "description", nullable = true, length = 50)
      private String description;
}
