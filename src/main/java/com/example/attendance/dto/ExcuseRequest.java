package com.example.attendance.dto;

import java.time.LocalDate;
import java.util.List;

public class ExcuseRequest {
    private List<LocalDate> dates;
    private String excuseType;
    private String description;

    public List<LocalDate> getDates() { return dates; }
    public void setDates(List<LocalDate> dates) { this.dates = dates; }

    public String getExcuseType() { return excuseType; }
    public void setExcuseType(String excuseType) { this.excuseType = excuseType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
