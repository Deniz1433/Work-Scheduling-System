// src/main/java/com/example/attendance/model/RoleNodePosition.java
package com.example.attendance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "role_node_position")
public class RoleNodePosition {
    @Id
    @Column(name = "role", length = 100)
    private String role;

    @Column(name = "pos_x", nullable = false)
    private double posX;

    @Column(name = "pos_y", nullable = false)
    private double posY;

    public RoleNodePosition() {}
    public RoleNodePosition(String role, double posX, double posY) {
        this.role = role; this.posX = posX; this.posY = posY;
    }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public double getPosX() { return posX; }
    public void setPosX(double posX) { this.posX = posX; }
    public double getPosY() { return posY; }
    public void setPosY(double posY) { this.posY = posY; }
}
