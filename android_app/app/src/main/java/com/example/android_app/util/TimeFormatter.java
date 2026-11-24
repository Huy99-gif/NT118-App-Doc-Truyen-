package com.example.android_app.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class TimeFormatter {
    private static final int ISO_SECONDS_LENGTH = 19; // "yyyy-MM-ddTHH:mm:ss"

    private TimeFormatter() {}

    // minSdk 24 chưa có java.time nên parse tay chuỗi Instant ISO (UTC) từ backend
    public static String formatIsoInstant(String iso, TimeZone zone) {
        if (iso == null || iso.length() < ISO_SECONDS_LENGTH) return "";
        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            parser.setLenient(false);
            Date date = parser.parse(iso.substring(0, ISO_SECONDS_LENGTH));
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.US);
            formatter.setTimeZone(zone);
            return formatter.format(date);
        } catch (ParseException exception) {
            return "";
        }
    }
}
