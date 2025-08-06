package com.example.attendance.util;

import jakarta.ws.rs.core.Response;

public class CreatedResponseUtil {
    public static String getCreatedId(Response response) {
        String location = response.getLocation().getPath();
        return location.substring(location.lastIndexOf('/') + 1);
    }
} 