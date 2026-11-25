package com.example.android_app.offline;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StorageFormatTest {
    @Test
    public void bytes_underOneKb() {
        assertEquals("512 B", StorageFormat.formatBytes(512));
    }

    @Test
    public void kilobytes_roundDown() {
        assertEquals("340 KB", StorageFormat.formatBytes(348160));
    }

    @Test
    public void megabytes_oneDecimal() {
        assertEquals("2.1 MB", StorageFormat.formatBytes(2202009));
    }
}
