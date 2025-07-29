package com.example.attendance.util;

import java.util.Collection;

/**
 * Parametre validasyonu için yardımcı fonksiyonlar.
 */
public final class ValidationUtils {
    private ValidationUtils() {}

    public static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }

    public static void requireNonEmpty(Collection<?> value, String message) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(message);
    }
} 