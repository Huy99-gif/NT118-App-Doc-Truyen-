package com.example.android_app.offline.work;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.android_app.offline.SyncOutcome;
import com.example.android_app.offline.db.DownloadedStoryEntity;

import java.util.concurrent.TimeUnit;

/** Đặt lịch các việc nền của tính năng offline: tải chương, tải cả truyện, đồng bộ. */
public final class OfflineWork {
    public static final String KEY_OWNER = "owner";
    public static final String KEY_STORY_ID = "storyId";
    public static final String KEY_CHAPTER_ID = "chapterId";
    public static final String KEY_TITLE = "title";
    public static final String KEY_AUTHOR = "author";
    public static final String KEY_COVER = "cover";
    public static final String KEY_TOTAL = "total";
    public static final String PROGRESS_DONE = "done";
    public static final String PROGRESS_TOTAL = "progressTotal";

    private static final long BACKOFF_SECONDS = 30;

    private OfflineWork() {}

    public static String storyTag(String owner, long storyId) {
        return "story-" + owner + "-" + storyId;
    }

    public static String chapterTag(long chapterId) {
        return "chapter-" + chapterId;
    }

    public static String storyWorkName(String owner, long storyId) {
        return "download-story-" + owner + "-" + storyId;
    }

    public static String chapterWorkName(String owner, long chapterId) {
        return "download-chapter-" + owner + "-" + chapterId;
    }

    public static String syncWorkName(String owner) {
        return "sync-" + owner;
    }

    public static void enqueueChapterDownload(Context context, String owner,
                                              DownloadedStoryEntity storyMeta, long chapterId) {
        Data input = storyData(owner, storyMeta).putLong(KEY_CHAPTER_ID, chapterId).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ChapterDownloadWorker.class)
                .setInputData(input)
                .setConstraints(networkConstraint())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .addTag(storyTag(owner, storyMeta.storyId))
                .addTag(chapterTag(chapterId))
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                chapterWorkName(owner, chapterId), ExistingWorkPolicy.KEEP, request);
    }

    public static void enqueueStoryDownload(Context context, String owner,
                                            DownloadedStoryEntity storyMeta) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(StoryDownloadWorker.class)
                .setInputData(storyData(owner, storyMeta).build())
                .setConstraints(networkConstraint())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .addTag(storyTag(owner, storyMeta.storyId))
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                storyWorkName(owner, storyMeta.storyId), ExistingWorkPolicy.KEEP, request);
    }

    public static void cancelStoryDownload(Context context, String owner, long storyId) {
        WorkManager.getInstance(context).cancelUniqueWork(storyWorkName(owner, storyId));
    }

    public static void enqueueSync(Context context, String owner) {
        if (owner == null) return;
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setInputData(new Data.Builder().putString(KEY_OWNER, owner).build())
                .setConstraints(networkConstraint())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                syncWorkName(owner), ExistingWorkPolicy.KEEP, request);
    }

    static DownloadedStoryEntity storyFrom(Data input, long now) {
        DownloadedStoryEntity story = new DownloadedStoryEntity();
        story.ownerKey = input.getString(KEY_OWNER);
        story.storyId = input.getLong(KEY_STORY_ID, 0);
        story.title = input.getString(KEY_TITLE);
        story.authorName = input.getString(KEY_AUTHOR);
        story.coverUrl = input.getString(KEY_COVER);
        story.totalChapters = input.getInt(KEY_TOTAL, 0);
        story.downloadedAt = now;
        return story;
    }

    /** Kết quả của worker theo mã HTTP: lỗi tạm thời thì thử lại, còn lại thì dừng. */
    static ListenableWorker.Result resultForHttpCode(int code) {
        SyncOutcome outcome = SyncOutcome.fromHttpCode(code);
        if (outcome == SyncOutcome.DONE) return ListenableWorker.Result.success();
        if (outcome == SyncOutcome.RETRY) return ListenableWorker.Result.retry();
        return ListenableWorker.Result.failure();
    }

    private static Data.Builder storyData(String owner, DownloadedStoryEntity storyMeta) {
        return new Data.Builder()
                .putString(KEY_OWNER, owner)
                .putLong(KEY_STORY_ID, storyMeta.storyId)
                .putString(KEY_TITLE, storyMeta.title)
                .putString(KEY_AUTHOR, storyMeta.authorName)
                .putString(KEY_COVER, storyMeta.coverUrl)
                .putInt(KEY_TOTAL, storyMeta.totalChapters);
    }

    private static Constraints networkConstraint() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }
}
