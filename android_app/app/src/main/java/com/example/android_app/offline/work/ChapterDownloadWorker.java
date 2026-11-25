package com.example.android_app.offline.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.android_app.model.ChapterDetailResponse;
import com.example.android_app.network.ApiClient;
import com.example.android_app.offline.db.DownloadedChapterEntity;
import com.example.android_app.offline.db.DownloadedStoryEntity;
import com.example.android_app.offline.db.OfflineDatabase;

import java.io.IOException;
import java.util.Collections;

import retrofit2.Response;

/** Tải một chương người dùng bấm ⬇ và lưu vào Room. */
public class ChapterDownloadWorker extends Worker {
    public ChapterDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String owner = getInputData().getString(OfflineWork.KEY_OWNER);
        long chapterId = getInputData().getLong(OfflineWork.KEY_CHAPTER_ID, 0);
        String authorization = ApiClient.authorization(getApplicationContext());
        if (owner == null || chapterId <= 0 || authorization == null) return Result.failure();

        try {
            Response<ChapterDetailResponse> response =
                    ApiClient.api().getChapter(authorization, chapterId).execute();
            if (!response.isSuccessful() || response.body() == null) {
                return OfflineWork.resultForHttpCode(response.isSuccessful() ? 500 : response.code());
            }
            long now = System.currentTimeMillis();
            DownloadedStoryEntity story = OfflineWork.storyFrom(getInputData(), now);
            DownloadedChapterEntity chapter =
                    DownloadedChapterEntity.from(owner, response.body(), story.storyId, now);
            OfflineDatabase.get(getApplicationContext()).dao()
                    .saveDownloaded(story, Collections.singletonList(chapter));
            return Result.success();
        } catch (IOException exception) {
            return Result.retry();
        }
    }
}
