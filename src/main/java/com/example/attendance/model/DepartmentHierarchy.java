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
@Table(name = "department_hierarchy", uniqueConstraints = {
        @UniqueConstraint(name = "uq_department_hierarchy", columnNames = {"parent_department_id", "child_department_id"})
})
public class DepartmentHierarchy {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name="parent_department_id", nullable=false)
    private Department parentDepartment;

    @ManyToOne
    @JoinColumn(name="child_department_id", nullable=false)
    private Department childDepartment;

    public DepartmentHierarchy(Department pDepartment, Department cDepartment){
      this.parentDepartment = pDepartment;
      this.childDepartment = cDepartment;
    }
} 
