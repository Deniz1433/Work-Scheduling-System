/**
 * RoleNodePosition, bir rolün ekrandaki pozisyonunu temsil eder.
 */
// src/main/java/com/example/attendance/model/RoleNodePosition.java
package com.example.attendance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "role_node_position")
public class RoleNodePosition {
    /** Rol adı */
    @Id
    @Column(name = "role", length = 100)
    private String role;

    /** X koordinatı */
    @Column(name = "pos_x", nullable = false)
    private double x;

    /** Y koordinatı */
    @Column(name = "pos_y", nullable = false)
    private double y;

    public RoleNodePosition() {}
    public RoleNodePosition(String role, double x, double y) {
        this.role = role; this.x = x; this.y = y;
    }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
}
