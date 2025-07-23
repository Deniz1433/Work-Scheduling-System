package com.example.attendance.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class ExcusesRequest {
    private List<LocalDate> dates;
    private int excuseType;
    private String description;
}
