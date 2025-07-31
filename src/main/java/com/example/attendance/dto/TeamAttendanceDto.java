package com.example.attendance.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TeamAttendanceDto {

    private Long id;
    private String name;
    private String surname;
    private String department;
    private Long departmentId;
    private List<Integer> attendance; // [Mon, Tue, Wed, Thu, Fri]
    private boolean isApproved;
    private String employeeExcuse;
    private String lastEditReason;
    private String lastEditDate;

    
}