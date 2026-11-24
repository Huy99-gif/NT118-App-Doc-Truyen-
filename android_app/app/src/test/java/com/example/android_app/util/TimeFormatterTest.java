package com.example.android_app.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.TimeZone;

public class TimeFormatterTest {
    private static final TimeZone VIETNAM = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");

    @Test
    public void formatsInstantWithFractionalSecondsInGivenZone() {
        assertEquals("10:15 24/11/2026",
                TimeFormatter.formatIsoInstant("2026-11-24T03:15:30.123456Z", VIETNAM));
    }

    @Test
    public void formatsInstantWithoutFractionalSeconds() {
        assertEquals("10:15 24/11/2026",
                TimeFormatter.formatIsoInstant("2026-11-24T03:15:30Z", VIETNAM));
    }

    @Test
    public void returnsEmptyForNullOrInvalidInput() {
        assertEquals("", TimeFormatter.formatIsoInstant(null, VIETNAM));
        assertEquals("", TimeFormatter.formatIsoInstant("abc", VIETNAM));
    }
}
