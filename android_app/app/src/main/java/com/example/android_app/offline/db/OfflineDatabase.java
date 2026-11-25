package com.example.android_app.offline.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        DownloadedStoryEntity.class,
        DownloadedChapterEntity.class,
        LocalProgressEntity.class,
        PendingProgressEntity.class,
        PendingBookmarkEntity.class
}, version = 1, exportSchema = false)
public abstract class OfflineDatabase extends RoomDatabase {
    private static volatile OfflineDatabase instance;

    public abstract OfflineDao dao();

    public static OfflineDatabase get(Context context) {
        if (instance == null) {
            synchronized (OfflineDatabase.class) {
                if (instance == null) {
                    // không dùng fallbackToDestructiveMigration để tránh mất dữ liệu chờ đồng bộ
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            OfflineDatabase.class, "offline.db").build();
                }
            }
        }
        return instance;
    }
}
