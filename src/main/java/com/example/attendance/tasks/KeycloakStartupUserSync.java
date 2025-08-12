package com.example.attendance.tasks;

import com.example.attendance.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakStartupUserSync implements ApplicationRunner {

    private final AdminService adminService;

    @Override
    public void run(ApplicationArguments args) {
        String wellKnown = "http://keycloak:8080/realms/attendance-realm/.well-known/openid-configuration";
        for (int i = 0; i < 30; i++) {
            if (isReachable(wellKnown)) {
                adminService.syncUsersFromKeycloak();
                return;
            }
            sleep(2000);
        }
    }

    private boolean isReachable(String url) {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(2000);
            return con.getResponseCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
