package com.example.android_app.offline.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;

import com.example.android_app.model.ChapterDetailResponse;

import java.nio.charset.StandardCharsets;

@Entity(tableName = "downloaded_chapter",
        primaryKeys = {"ownerKey", "chapterId"},
        indices = {@Index({"ownerKey", "storyId"})})
public class DownloadedChapterEntity {
    @NonNull
    public String ownerKey = "";
    public long chapterId;
    public long storyId;
    public int chapterNo;
    public String title;
    public String content;
    public String updatedAt;
    public long downloadedAt;
    public long sizeBytes;

    public static DownloadedChapterEntity from(String ownerKey, ChapterDetailResponse chapter,
                                               long storyIdFallback, long now) {
        DownloadedChapterEntity entity = new DownloadedChapterEntity();
        entity.ownerKey = ownerKey;
        entity.chapterId = chapter.id == null ? 0 : chapter.id;
        entity.storyId = chapter.storyId == null ? storyIdFallback : chapter.storyId;
        entity.chapterNo = chapter.chapterNo == null ? 0 : chapter.chapterNo;
        entity.title = chapter.title;
        entity.content = chapter.content == null ? "" : chapter.content;
        entity.updatedAt = chapter.updatedAt;
        entity.downloadedAt = now;
        entity.sizeBytes = entity.content.getBytes(StandardCharsets.UTF_8).length;
        return entity;
    }
}
