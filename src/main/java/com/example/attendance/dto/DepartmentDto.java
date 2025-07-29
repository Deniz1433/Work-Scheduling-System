package com.example.attendance.dto;

import com.example.attendance.model.Department;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentDto {
    private Long id;
    private String name;
    private int minDays;
    private Long childDepartmentId;

    public DepartmentDto(Department department) {
        this.id = department.getId();
        this.name = department.getName();
        this.minDays = department.getMinDays();
        this.childDepartmentId = department.getChildDepartment() != null ? department.getChildDepartment().getId() : null;
    }
}
