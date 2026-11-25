package com.example.android_app.offline;

import android.content.Context;
import android.content.SharedPreferences;

/** Lưu các lỗi đồng bộ (dấu trang/vị trí đọc bị server từ chối) để báo cho người dùng sau. */
public final class SyncErrorStore {
    private static final String PREFS_NAME = "offline_sync";
    private static final String KEY_ERRORS = "errors";

    private SyncErrorStore() {}

    public static synchronized void record(Context context, String message) {
        SharedPreferences preferences = prefs(context);
        String existing = preferences.getString(KEY_ERRORS, null);
        String value = existing == null ? message : existing + "\n" + message;
        preferences.edit().putString(KEY_ERRORS, value).commit();
    }

    /** Trả về các lỗi đã ghi (nối bằng xuống dòng) rồi xoá; null nếu không có. */
    public static synchronized String consume(Context context) {
        SharedPreferences preferences = prefs(context);
        String value = preferences.getString(KEY_ERRORS, null);
        if (value != null) preferences.edit().remove(KEY_ERRORS).commit();
        return value;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
