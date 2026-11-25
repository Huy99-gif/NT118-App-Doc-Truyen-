package com.example.android_app.offline;

import java.util.Locale;

public final class StorageFormat {
    private static final long KB = 1024;
    private static final long MB = KB * 1024;

    private StorageFormat() {}

    public static String formatBytes(long bytes) {
        if (bytes < KB) return bytes + " B";
        if (bytes < MB) return (bytes / KB) + " KB";
        return String.format(Locale.ROOT, "%.1f MB", bytes / (double) MB);
    }
}
