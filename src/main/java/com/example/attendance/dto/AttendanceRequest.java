package com.example.attendance.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceRequest {
    private String userId;
    private String weekStart;
    private List<Integer> dates; // 5 elemanlı, günlerin durumunu tutan liste
    private String explanation; // E-posta gönderilecekse açıklama metni
}   
