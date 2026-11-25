package com.example.android_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AuthPreferencesTest {
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @After
    public void tearDown() {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void saveOwnerKey_normalizes() {
        AuthPreferences.saveOwnerKey(context, " A@X.com ");

        assertEquals("a@x.com", AuthPreferences.getOwnerKey(context));
    }

    @Test
    public void clear_keepsOwnerKey_removesToken() {
        AuthPreferences.saveToken(context, "token");
        AuthPreferences.saveOwnerKey(context, "a@x.com");

        AuthPreferences.clear(context);

        assertNull(AuthPreferences.getToken(context));
        assertEquals("a@x.com", AuthPreferences.getOwnerKey(context));
    }
}
