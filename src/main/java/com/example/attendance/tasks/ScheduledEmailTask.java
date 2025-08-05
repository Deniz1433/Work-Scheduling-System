package com.example.attendance.tasks;

import com.example.attendance.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ScheduledEmailTask {

    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "0 1-59 14 * * *") // 13:34, 13:35, ..., 13:40
    public void sendScheduledEmail() {
        emailService.sendEmail(
            "sahinemi22@itu.edu.tr",
            "Otomatik E-posta",
            "Bu, attendance-app'tan gönderilen otomatik e-postadır."
        );
    }
}