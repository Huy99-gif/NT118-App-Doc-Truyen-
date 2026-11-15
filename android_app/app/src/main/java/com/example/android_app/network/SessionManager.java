package com.example.android_app.network;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.example.android_app.AuthPreferences;
import com.example.android_app.LoginActivity;

import java.util.concurrent.atomic.AtomicBoolean;

public final class SessionManager {
    public static final String EXTRA_SESSION_EXPIRED =
            "com.example.android_app.EXTRA_SESSION_EXPIRED";

    private final Context applicationContext;
    private final AtomicBoolean logoutStarted = new AtomicBoolean(false);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SessionManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context không được null");
        }
        this.applicationContext = context.getApplicationContext();
    }

    public synchronized void onLoginSuccess(String token) {
        AuthPreferences.saveToken(applicationContext, token);
        AuthPreferences.setSessionExpired(applicationContext, false);
        logoutStarted.set(false);
    }

    public void handleUnauthorized(String requestAuthorization) {
        synchronized (this) {
            String currentToken = AuthPreferences.getToken(applicationContext);
            if (currentToken == null
                    || !("Bearer " + currentToken).equals(requestAuthorization)
                    || !logoutStarted.compareAndSet(false, true)) {
                return;
            }
            AuthPreferences.clear(applicationContext);
            AuthPreferences.setSessionExpired(applicationContext, true);
        }

        mainHandler.post(() -> {
            synchronized (this) {
                if (AuthPreferences.getToken(applicationContext) != null) {
                    return;
                }
                Intent intent = new Intent(applicationContext, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra(EXTRA_SESSION_EXPIRED, true);
                try {
                    applicationContext.startActivity(intent);
                } catch (Exception ignored) {
                    // Mở Activity từ background có thể bị chặn trên Android 10+;
                    // App.java's ActivityLifecycleCallbacks sẽ tự chuyển hướng khi Activity bất kỳ trở lại foreground.
                }
            }
        });
    }
}
