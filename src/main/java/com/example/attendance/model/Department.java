package com.example.attendance.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
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

      @Override
      public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Department that = (Department) o;
            return Objects.equals(id, that.id);
      }

      @Override
      public int hashCode() {
            return Objects.hash(id);
      }

      @Override
      public String toString() {
            return "Department{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", minDays=" + minDays +
                    '}';
      }
}
