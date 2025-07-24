package com.example.demo;

import com.example.automation.model.User;
import com.example.automation.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ReminderScheduler {

    @Autowired
    private EmailService emailService;

    
    @Scheduled(cron = "${scheduler.reminder.cron}")
    public void sendWeeklyReminders() {
        List<User> users = mockUsers(); 
        for (User user : users) {
            if (!user.hasSelectedDays()) {
                emailService.sendReminderEmail(
                    user.getEmail(),
                    "Ofis Günü Seçimi Hatırlatma",
                    user.getName() + ", lütfen ofiste bulunacağınız günleri seçin."
                );
            } else if (!user.isApproved()) {
                emailService.sendReminderEmail(
                    user.getEmail(),
                    "Onay Bekleyen Kayıt",
                    user.getName() + ", seçtiğiniz günler henüz ekip lideri tarafından onaylanmadı."
                );
            }
        }
    }


    private List<User> mockUsers() {
        return Arrays.asList(
            new User("Ahmet", "efemandal@gmail.com", false, false),
            new User("Ayşe", "efemandal@gmail.com", true, false),
            new User("Mehmet", "efemandal@gmail.com", true, true)
        );
    }
}