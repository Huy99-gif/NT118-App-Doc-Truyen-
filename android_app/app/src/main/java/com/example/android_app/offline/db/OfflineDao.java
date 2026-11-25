package com.example.android_app.offline.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Upsert;

import java.util.List;

@Dao
public abstract class OfflineDao {
    @Upsert
    public abstract void upsertStory(DownloadedStoryEntity story);

    @Upsert
    public abstract void upsertChapters(List<DownloadedChapterEntity> chapters);

    @Transaction
    public void saveDownloaded(DownloadedStoryEntity story, List<DownloadedChapterEntity> chapters) {
        upsertStory(story);
        upsertChapters(chapters);
    }

    @Query("SELECT * FROM downloaded_chapter WHERE ownerKey = :ownerKey AND chapterId = :chapterId")
    public abstract DownloadedChapterEntity findChapter(String ownerKey, long chapterId);

    @Query("SELECT * FROM downloaded_story WHERE ownerKey = :ownerKey AND storyId = :storyId")
    public abstract DownloadedStoryEntity findStory(String ownerKey, long storyId);

    @Query("SELECT chapterId, storyId, chapterNo, title, updatedAt, sizeBytes FROM downloaded_chapter"
            + " WHERE ownerKey = :ownerKey AND storyId = :storyId ORDER BY chapterNo")
    public abstract List<ChapterHeader> chapterHeaders(String ownerKey, long storyId);

    @Query("SELECT chapterId, storyId, chapterNo, title, updatedAt, sizeBytes FROM downloaded_chapter"
            + " WHERE ownerKey = :ownerKey AND storyId = :storyId ORDER BY chapterNo")
    public abstract LiveData<List<ChapterHeader>> observeChapterHeaders(String ownerKey, long storyId);

    @Query("SELECT s.storyId, s.title, s.authorName, s.coverUrl, s.totalChapters,"
            + " COUNT(c.chapterId) AS chapterCount, COALESCE(SUM(c.sizeBytes), 0) AS totalBytes"
            + " FROM downloaded_story s JOIN downloaded_chapter c"
            + " ON c.ownerKey = s.ownerKey AND c.storyId = s.storyId"
            + " WHERE s.ownerKey = :ownerKey"
            + " GROUP BY s.storyId ORDER BY s.downloadedAt DESC")
    public abstract LiveData<List<DownloadedStorySummary>> observeStories(String ownerKey);

    @Query("SELECT COUNT(*) FROM downloaded_chapter WHERE ownerKey = :ownerKey")
    public abstract int countChapters(String ownerKey);

    @Query("DELETE FROM downloaded_chapter WHERE ownerKey = :ownerKey AND chapterId = :chapterId")
    abstract void deleteChapterRow(String ownerKey, long chapterId);

    @Query("DELETE FROM downloaded_story WHERE ownerKey = :ownerKey AND storyId = :storyId"
            + " AND NOT EXISTS (SELECT 1 FROM downloaded_chapter c"
            + " WHERE c.ownerKey = :ownerKey AND c.storyId = :storyId)")
    abstract void deleteStoryIfEmpty(String ownerKey, long storyId);

    @Query("DELETE FROM downloaded_chapter WHERE ownerKey = :ownerKey AND storyId = :storyId")
    abstract void deleteChaptersOfStory(String ownerKey, long storyId);

    @Query("DELETE FROM downloaded_story WHERE ownerKey = :ownerKey AND storyId = :storyId")
    abstract void deleteStoryRow(String ownerKey, long storyId);

    /** Xoá một chương; nếu truyện không còn chương nào thì xoá luôn truyện. */
    @Transaction
    public void deleteChapter(String ownerKey, long storyId, long chapterId) {
        deleteChapterRow(ownerKey, chapterId);
        deleteStoryIfEmpty(ownerKey, storyId);
    }

    @Transaction
    public void deleteStory(String ownerKey, long storyId) {
        deleteChaptersOfStory(ownerKey, storyId);
        deleteStoryRow(ownerKey, storyId);
    }

    @Upsert
    public abstract void upsertLocalProgress(LocalProgressEntity progress);

    @Query("SELECT * FROM local_progress WHERE ownerKey = :ownerKey AND storyId = :storyId")
    public abstract LocalProgressEntity findLocalProgress(String ownerKey, long storyId);

    @Upsert
    public abstract void upsertPendingProgress(PendingProgressEntity progress);

    @Query("SELECT * FROM pending_progress WHERE ownerKey = :ownerKey ORDER BY updatedAt")
    public abstract List<PendingProgressEntity> pendingProgress(String ownerKey);

    /** Chỉ xoá khi updatedAt chưa đổi, để không làm mất vị trí mới ghi trong lúc đang gửi. */
    @Query("DELETE FROM pending_progress WHERE ownerKey = :ownerKey AND storyId = :storyId"
            + " AND updatedAt = :updatedAt")
    public abstract int deletePendingProgressIfUnchanged(String ownerKey, long storyId, long updatedAt);

    @Insert
    public abstract long insertPendingBookmark(PendingBookmarkEntity bookmark);

    @Query("SELECT * FROM pending_bookmark WHERE ownerKey = :ownerKey ORDER BY createdAt")
    public abstract List<PendingBookmarkEntity> pendingBookmarks(String ownerKey);

    @Query("DELETE FROM pending_bookmark WHERE localId = :localId")
    public abstract void deletePendingBookmark(long localId);

    @Query("SELECT (SELECT COUNT(*) FROM pending_progress WHERE ownerKey = :ownerKey)"
            + " + (SELECT COUNT(*) FROM pending_bookmark WHERE ownerKey = :ownerKey)")
    public abstract int countPending(String ownerKey);
}
