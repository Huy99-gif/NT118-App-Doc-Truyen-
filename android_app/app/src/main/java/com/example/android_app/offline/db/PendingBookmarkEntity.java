package com.example.android_app.offline.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_bookmark", indices = {@Index("ownerKey")})
public class PendingBookmarkEntity {
    @PrimaryKey(autoGenerate = true)
    public long localId;
    @NonNull
    public String ownerKey = "";
    public long storyId;
    public long chapterId;
    public int chapterNo;
    public String chapterTitle;
    public String storyTitle;
    public int charOffset;
    public String note;
    public long createdAt;
}
