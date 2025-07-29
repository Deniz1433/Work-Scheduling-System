package com.example.attendance;

import java.util.List;
import java.util.Set;

/**
 * Uygulama genelinde kullanılan sabitler.
 */
public final class AppConstants {
    private AppConstants() {}

    public static final String CLIENT = "attendance-client";
    public static final List<String> BASE_ROLES = List.of("admin", "superadmin", "user");
    public static final Set<String> BASE = Set.of("admin", "user", "superadmin");
} 