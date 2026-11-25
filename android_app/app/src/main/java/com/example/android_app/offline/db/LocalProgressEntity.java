package com.example.android_app.offline.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/** Vị trí đọc gần nhất trên máy; không bị xoá khi đồng bộ. */
@Entity(tableName = "local_progress", primaryKeys = {"ownerKey", "storyId"})
public class LocalProgressEntity {
    @NonNull
    public String ownerKey = "";
    public long storyId;
    public long chapterId;
    public int charOffset;
    public long updatedAt;
}
