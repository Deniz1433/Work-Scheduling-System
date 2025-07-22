package com.example.attendance.dto;

import java.util.List;

public class TeamAttendanceDto {

    private Long id;
    private String name;
    private String surname;
    private String department;
    private List<Boolean> attendance; // [Mon, Tue, Wed, Thu, Fri]
    private boolean isApproved;
    private String employeeExcuse;
    private String lastEditReason;
    private String lastEditDate;

    public TeamAttendanceDto() {}

    public TeamAttendanceDto(Long id, String name, String surname, String department,
                             List<Boolean> attendance, boolean isApproved,
                             String employeeExcuse, String lastEditReason, String lastEditDate) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.department = department;
        this.attendance = attendance;
        this.isApproved = isApproved;
        this.employeeExcuse = employeeExcuse;
        this.lastEditReason = lastEditReason;
        this.lastEditDate = lastEditDate;
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public List<Boolean> getAttendance() {
        return attendance;
    }

    public void setAttendance(List<Boolean> attendance) {
        this.attendance = attendance;
    }

    public boolean isApproved() {
        return isApproved;
    }

    public void setApproved(boolean approved) {
        isApproved = approved;
    }

    public String getEmployeeExcuse() {
        return employeeExcuse;
    }

    public void setEmployeeExcuse(String employeeExcuse) {
        this.employeeExcuse = employeeExcuse;
    }

    public String getLastEditReason() {
        return lastEditReason;
    }

    public void setLastEditReason(String lastEditReason) {
        this.lastEditReason = lastEditReason;
    }

    public String getLastEditDate() {
        return lastEditDate;
    }

    public void setLastEditDate(String lastEditDate) {
        this.lastEditDate = lastEditDate;
    }
}