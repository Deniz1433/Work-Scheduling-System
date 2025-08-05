package com.example.attendance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        message.setFrom("emr.sahin35@gmail.com");

        try {
            mailSender.send(message);
            System.out.println("E-posta gönderildi: " + to);
        } catch (Exception e) {
            System.err.println("E-posta gönderimi başarısız: " + e.getMessage());
        }
    }
}