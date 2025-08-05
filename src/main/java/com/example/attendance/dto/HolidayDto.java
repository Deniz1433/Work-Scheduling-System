package com.example.attendance.dto;

import lombok.Getter;
import lombok.Setter;

import lombok.AllArgsConstructor;

@Getter
@Setter
@AllArgsConstructor
public class HolidayDto {
      public String date;
      public String localName;
      public String name;
      public String countryCode;
}
