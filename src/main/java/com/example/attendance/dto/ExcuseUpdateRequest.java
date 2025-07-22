package com.example.attendance.dto;

public class ExcuseUpdateRequest {
    private String excuseType;
    private String description;

    public String getExcuseType() { return excuseType; }
    public void setExcuseType(String excuseType) { this.excuseType = excuseType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
