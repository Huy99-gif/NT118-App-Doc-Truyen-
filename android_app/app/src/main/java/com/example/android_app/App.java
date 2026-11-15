package com.example.android_app;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import com.example.android_app.network.ApiClient;
import com.example.android_app.network.SessionManager;

public final class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.initialize(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                if (activity instanceof LoginActivity
                        || !AuthPreferences.isSessionExpired(App.this)
                        || AuthPreferences.getToken(App.this) != null) {
                    return;
                }

                Intent intent = new Intent(activity, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra(SessionManager.EXTRA_SESSION_EXPIRED, true);
                activity.startActivity(intent);
                AuthPreferences.setSessionExpired(App.this, false);
            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(Activity activity) {}

            @Override
            public void onActivityPaused(Activity activity) {}

            @Override
            public void onActivityStopped(Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override
            public void onActivityDestroyed(Activity activity) {}
        });
    }
}
