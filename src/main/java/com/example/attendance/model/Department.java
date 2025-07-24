package com.example.attendance.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "department")
public class Department {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;
      @Column(name = "name", nullable = false, length = 36)
      private String name;
      @Column(name = "min_days", nullable = false, length = 2)
      private int minDays;// ofiste minimum gün
}
