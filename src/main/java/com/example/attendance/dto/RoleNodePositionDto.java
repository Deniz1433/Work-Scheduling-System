package com.example.attendance.dto;

/**
 * RoleNodePositionDto, rolün pozisyon bilgisini taşır.
 */
public class RoleNodePositionDto {
    /** Rol adı */
    private String role;
    /** X koordinatı */
    private double x;
    /** Y koordinatı */
    private double y;

    public RoleNodePositionDto(String role, double x, double y) {
        this.role = role;
        this.x = x;
        this.y = y;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
} 