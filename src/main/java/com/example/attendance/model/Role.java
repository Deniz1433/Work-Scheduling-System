package com.example.attendance.model;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "role")
public class Role{
      @Id
      private Long id;
      @Column(name = "name", nullable = false, length = 36)
      private String name;
      @Column(name = "can_edit", nullable = false)
      private Boolean canEdit;
}