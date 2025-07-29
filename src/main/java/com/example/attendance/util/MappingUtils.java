package com.example.attendance.util;

import com.example.attendance.dto.RoleHierarchyDto;
import com.example.attendance.dto.RoleNodePositionDto;
import com.example.attendance.model.RoleHierarchy;
import com.example.attendance.model.RoleNodePosition;

/**
 * Entity'den DTO'ya mapping işlemleri için yardımcı fonksiyonlar.
 */
public final class MappingUtils {
    private MappingUtils() {}

    public static RoleHierarchyDto toDto(RoleHierarchy entity) {
        return new RoleHierarchyDto(entity.getParentRole(), entity.getChildRole());
    }

    public static RoleNodePositionDto toDto(RoleNodePosition entity) {
        return new RoleNodePositionDto(entity.getRole(), entity.getX(), entity.getY());
    }
} 