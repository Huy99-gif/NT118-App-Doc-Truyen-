package com.example.android_app;

import android.content.Context;
import android.content.SharedPreferences;

public final class AuthPreferences {
    private static final String PREFS_NAME = "auth";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_SESSION_EXPIRED = "sessionExpired";

    private AuthPreferences() {}

    public static void saveToken(Context context, String token) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public static String getToken(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_TOKEN, null);
    }

    public static boolean isSessionExpired(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SESSION_EXPIRED, false);
    }

    public static void setSessionExpired(Context context, boolean expired) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SESSION_EXPIRED, expired)
                .apply();
    }

    public static void clear(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_TOKEN)
                .apply();
    }
}
