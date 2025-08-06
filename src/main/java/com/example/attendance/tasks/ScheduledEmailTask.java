package com.example.attendance.tasks;

import com.example.attendance.dto.UserDto;
import com.example.attendance.model.Attendance;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.service.EmailService;
import com.example.attendance.service.UserService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ScheduledEmailTask {

    @Autowired
    private EmailService emailService;
    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private UserService userService;

    private String getAttendanceStatusString(List<Integer> dates) {
        String[] statusMap = {
            "Veri girişi yok",    // 0
            "Ofiste",             // 1
            "Uzaktan",            // 2
            "İzinli",             // 3
            "(Yıllık İzin olmadan) Mazeretli", // 4
            "Resmi Tatil"         // 5
        };
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dates.size(); i++) {
            int status = dates.get(i);
            String statusStr = (status >= 0 && status < statusMap.length) ? statusMap[status] : "Bilinmeyen";
            sb.append((i + 1) + ". gün: " + statusStr + "\n");
        }
        return sb.toString();
    }
    @Scheduled(cron = "0 0 5 ? * FRI") // Cuma günü saat sabah saat 8  
    public void sendScheduledEmail() {
        List<Attendance> attendances = attendanceService.getAllAttendance();

        for(Attendance a : attendances){
            if(a.getDates().contains(0)){
                UserDto u = userService.getUserById(a.getUserId());
                String email = u.getEmail();
                //lokal users tablosundan eposta çekilir
                String body = "Bu, ofis günlerinizi kaydetmediğiniz için gönderilen e-postadır. Lütfen bilgi girişi yapın.\n\n"
                    + "Mevcut attendance durumunuz: " + getAttendanceStatusString(a.getDates());
                emailService.sendEmail(
                    email,
                    "Ofis Günü Kayıt Hatırlatması",
                    body
                );
            }
        }
    }
}