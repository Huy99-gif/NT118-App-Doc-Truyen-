package com.example.android_app.offline;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Luồng nền dùng chung cho mọi truy cập Room từ giao diện. */
public final class OfflineExecutors {
    private static final Executor IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private OfflineExecutors() {}

    public static Executor io() {
        return IO;
    }

    public static void main(Runnable runnable) {
        MAIN.post(runnable);
    }
}
