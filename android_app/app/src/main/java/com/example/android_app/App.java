package com.example.android_app;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;

import com.example.android_app.network.ApiClient;
import com.example.android_app.network.SessionManager;
import com.example.android_app.offline.OfflineExecutors;
import com.example.android_app.offline.db.OfflineDatabase;
import com.example.android_app.offline.work.OfflineWork;

public final class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.initialize(this);
        syncPendingIfAny();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityResumed(Activity activity) {
                if (activity instanceof LoginActivity
                        || isOfflineReading(activity)
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

    // đọc truyện đã tải khi không còn phiên: không đẩy về màn đăng nhập
    private static boolean isOfflineReading(Activity activity) {
        if (activity instanceof OfflineLibraryActivity) return true;
        return activity instanceof ReaderActivity
                && activity.getIntent().getBooleanExtra(ReaderActivity.EXTRA_OFFLINE_MODE, false);
    }

    // còn vị trí đọc / dấu trang chờ gửi từ lần offline trước thì đặt lịch đồng bộ
    private void syncPendingIfAny() {
        String owner = AuthPreferences.getOwnerKey(this);
        if (owner == null || AuthPreferences.getToken(this) == null) return;
        OfflineExecutors.io().execute(() -> {
            if (OfflineDatabase.get(this).dao().countPending(owner) > 0) {
                OfflineWork.enqueueSync(this, owner);
            }
        });
    }
}
