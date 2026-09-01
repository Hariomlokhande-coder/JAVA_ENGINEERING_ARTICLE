package com.technicalblog.util;

/** Small helpers for normalising optional text coming from the API. */
public final class TextUtils {

    private TextUtils() {
    }

    /** Trims the value and converts blank input to null so optional columns stay clean. */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
