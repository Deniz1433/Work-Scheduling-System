// src/main/java/com/example/attendance/model/RoleNodePosition.java
package com.example.attendance.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "department_node_position")
public class DepartmentNodePosition {
    @Id
    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "pos_x", nullable = false)
    private double posX;

    @Column(name = "pos_y", nullable = false)
    private double posY;

}