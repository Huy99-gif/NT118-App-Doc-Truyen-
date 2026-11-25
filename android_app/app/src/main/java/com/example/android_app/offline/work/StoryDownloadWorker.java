package com.example.android_app.offline.work;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.android_app.model.ChapterDetailResponse;
import com.example.android_app.model.ChapterSummaryResponse;
import com.example.android_app.model.PageResponse;
import com.example.android_app.network.ApiClient;
import com.example.android_app.offline.ChapterBatcher;
import com.example.android_app.offline.DownloadPlanner;
import com.example.android_app.offline.db.ChapterHeader;
import com.example.android_app.offline.db.DownloadedChapterEntity;
import com.example.android_app.offline.db.DownloadedStoryEntity;
import com.example.android_app.offline.db.OfflineDao;
import com.example.android_app.offline.db.OfflineDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

/**
 * "Tải cả truyện": lấy đủ mọi trang danh sách chương, chỉ tải chương còn thiếu hoặc đã đổi,
 * theo lô 20 chương và ghi vào Room sau mỗi lô để chạy lại không phải tải từ đầu.
 */
public class StoryDownloadWorker extends Worker {
    public StoryDownloadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String owner = getInputData().getString(OfflineWork.KEY_OWNER);
        long storyId = getInputData().getLong(OfflineWork.KEY_STORY_ID, 0);
        String authorization = ApiClient.authorization(getApplicationContext());
        if (owner == null || storyId <= 0 || authorization == null) return Result.failure();
        OfflineDao dao = OfflineDatabase.get(getApplicationContext()).dao();

        try {
            List<ChapterSummaryResponse> remote = new ArrayList<>();
            long totalElements = 0;
            int pages = 1;
            for (int page = 0; page < pages; page++) {
                if (isStopped()) return Result.failure();
                Response<PageResponse<ChapterSummaryResponse>> response = ApiClient.api()
                        .getStoryChapters(authorization, storyId, page, DownloadPlanner.PAGE_SIZE)
                        .execute();
                if (!response.isSuccessful() || response.body() == null) {
                    return OfflineWork.resultForHttpCode(response.isSuccessful() ? 500 : response.code());
                }
                totalElements = response.body().totalElements;
                pages = DownloadPlanner.pageCount(totalElements, DownloadPlanner.PAGE_SIZE);
                remote.addAll(response.body().items);
            }

            Map<Long, String> localUpdatedAt = new HashMap<>();
            for (ChapterHeader header : dao.chapterHeaders(owner, storyId)) {
                localUpdatedAt.put(header.chapterId, header.updatedAt);
            }
            List<Long> toFetch = DownloadPlanner.chaptersToFetch(remote, localUpdatedAt);

            DownloadedStoryEntity story = OfflineWork.storyFrom(getInputData(), System.currentTimeMillis());
            story.totalChapters = (int) Math.min(Integer.MAX_VALUE, totalElements);
            int done = 0;
            reportProgress(done, toFetch.size());
            for (List<Long> batch : ChapterBatcher.chunk(toFetch, ChapterBatcher.MAX_BATCH)) {
                if (isStopped()) return Result.failure();
                Response<List<ChapterDetailResponse>> response = ApiClient.api()
                        .getChaptersBatch(authorization, TextUtils.join(",", batch))
                        .execute();
                if (!response.isSuccessful() || response.body() == null) {
                    return OfflineWork.resultForHttpCode(response.isSuccessful() ? 500 : response.code());
                }
                long now = System.currentTimeMillis();
                List<DownloadedChapterEntity> entities = new ArrayList<>();
                for (ChapterDetailResponse chapter : response.body()) {
                    entities.add(DownloadedChapterEntity.from(owner, chapter, storyId, now));
                }
                dao.saveDownloaded(story, entities);
                done += batch.size();
                reportProgress(done, toFetch.size());
            }
            return Result.success();
        } catch (IOException exception) {
            return Result.retry();
        }
    }

    private void reportProgress(int done, int total) {
        setProgressAsync(new Data.Builder()
                .putInt(OfflineWork.PROGRESS_DONE, done)
                .putInt(OfflineWork.PROGRESS_TOTAL, total)
                .build());
    }
}
