package com.example.attendance.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExcuseUpdateRequest {
    private Long id;
    private Integer excuseType;
    private String description;
}