package com.example.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ExcuseDto {
      private Long id;
      private Long userId;
      private String excuseDate;
      private int excuseType;
      private String description;
      private Boolean isApproved;
}
