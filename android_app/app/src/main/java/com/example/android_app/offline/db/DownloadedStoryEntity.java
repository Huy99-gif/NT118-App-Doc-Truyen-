package com.example.android_app.offline.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "downloaded_story", primaryKeys = {"ownerKey", "storyId"})
public class DownloadedStoryEntity {
    @NonNull
    public String ownerKey = "";
    public long storyId;
    public String title;
    public String authorName;
    public String coverUrl;
    public int totalChapters;
    public long downloadedAt;
}
