// src/main/java/com/example/attendance/model/RoleHierarchy.java
package com.example.attendance.model;

import jakarta.persistence.*;

@Entity
@Table(name = "role_hierarchy", uniqueConstraints = {
        @UniqueConstraint(name = "uq_role_hierarchy", columnNames = {"parent_role", "child_role"})
})
public class RoleHierarchy {
    @Id @GeneratedValue private Long id;

    @Column(name="parent_role", nullable=false)
    private String parentRole;

    @Column(name="child_role", nullable=false)
    private String childRole;

    // constructors, getters/setters
    public RoleHierarchy() {}
    public RoleHierarchy(String parentRole, String childRole) {
        this.parentRole = parentRole;
        this.childRole = childRole;
    }
    public Long getId() { return id; }
    public String getParentRole() { return parentRole; }
    public void setParentRole(String parentRole) { this.parentRole = parentRole; }
    public String getChildRole() { return childRole; }
    public void setChildRole(String childRole) { this.childRole = childRole; }
}
