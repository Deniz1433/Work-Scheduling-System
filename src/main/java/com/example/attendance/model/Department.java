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
      private Long id;
      @Column(name = "name", nullable = false, length = 36)
      private String name;
      @Column(name = "min_day", nullable = false, length = 2)
      private int min_day;// ofiste minimum gün
}
