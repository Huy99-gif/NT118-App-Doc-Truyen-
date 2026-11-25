package com.example.android_app.offline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SyncErrorStoreTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        SyncErrorStore.consume(context);
    }

    @Test
    public void consume_joinsErrorsAndClears() {
        SyncErrorStore.record(context, "lỗi 1");
        SyncErrorStore.record(context, "lỗi 2");

        assertEquals("lỗi 1\nlỗi 2", SyncErrorStore.consume(context));
        assertNull(SyncErrorStore.consume(context));
    }
}
