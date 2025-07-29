package com.example.attendance.dto;

/**
 * RoleHierarchyDto, rol hiyerarşisi bilgisini taşır.
 */
public class RoleHierarchyDto {
    /** Ebeveyn rol adı */
    private String parentRole;
    /** Çocuk rol adı */
    private String childRole;

    public RoleHierarchyDto(String parentRole, String childRole) {
        this.parentRole = parentRole;
        this.childRole = childRole;
    }

    public String getParentRole() {
        return parentRole;
    }

    public void setParentRole(String parentRole) {
        this.parentRole = parentRole;
    }

    public String getChildRole() {
        return childRole;
    }

    public void setChildRole(String childRole) {
        this.childRole = childRole;
    }
} 