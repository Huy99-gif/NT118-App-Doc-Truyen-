package com.example.android_app.offline.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/** Hàng đợi vị trí đọc chờ gửi lên server; mỗi truyện một dòng, xoá khi gửi thành công. */
@Entity(tableName = "pending_progress", primaryKeys = {"ownerKey", "storyId"})
public class PendingProgressEntity {
    @NonNull
    public String ownerKey = "";
    public long storyId;
    public long chapterId;
    public int charOffset;
    public long updatedAt;
}
