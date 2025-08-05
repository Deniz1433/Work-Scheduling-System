package com.example.attendance.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class ExcusesRequest {
    private List<String> dates;
    private int excuseType;
    private String description;
}
