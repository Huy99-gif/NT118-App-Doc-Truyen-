package com.example.android_app.offline.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.android_app.AuthPreferences;
import com.example.android_app.model.CreateBookmarkRequest;
import com.example.android_app.model.UpdateReadingHistoryRequest;
import com.example.android_app.network.ApiClient;
import com.example.android_app.offline.SyncErrorStore;
import com.example.android_app.offline.SyncOutcome;
import com.example.android_app.offline.db.OfflineDao;
import com.example.android_app.offline.db.OfflineDatabase;
import com.example.android_app.offline.db.PendingBookmarkEntity;
import com.example.android_app.offline.db.PendingProgressEntity;
import com.example.android_app.util.ApiErrorParser;

import java.io.IOException;

import retrofit2.Response;

/** Gửi vị trí đọc và dấu trang tạo lúc offline lên server khi có mạng. */
public class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        String owner = getInputData().getString(OfflineWork.KEY_OWNER);
        String authorization = ApiClient.authorization(context);
        // chỉ gửi dữ liệu của đúng tài khoản đang đăng nhập; khác thì giữ nguyên chờ người đó quay lại
        if (owner == null || authorization == null
                || !owner.equals(AuthPreferences.getOwnerKey(context))) {
            return Result.failure();
        }
        OfflineDao dao = OfflineDatabase.get(context).dao();

        try {
            for (PendingProgressEntity progress : dao.pendingProgress(owner)) {
                if (isStopped()) return Result.retry();
                Response<?> response = ApiClient.api().updateReadingHistory(authorization,
                        new UpdateReadingHistoryRequest(progress.storyId, progress.chapterId,
                                progress.charOffset)).execute();
                SyncOutcome outcome = SyncOutcome.withAttempts(
                        SyncOutcome.fromHttpCode(response.code()), getRunAttemptCount());
                if (outcome == SyncOutcome.RETRY) return Result.retry();
                if (outcome == SyncOutcome.STOP_AUTH) return Result.failure();
                if (outcome == SyncOutcome.DROP) {
                    SyncErrorStore.record(context, "Không đồng bộ được vị trí đọc: "
                            + ApiErrorParser.getMessage(response, "server từ chối"));
                }
                dao.deletePendingProgressIfUnchanged(owner, progress.storyId, progress.updatedAt);
            }

            for (PendingBookmarkEntity bookmark : dao.pendingBookmarks(owner)) {
                if (isStopped()) return Result.retry();
                Response<?> response = ApiClient.api().createBookmark(authorization,
                        new CreateBookmarkRequest(bookmark.chapterId, bookmark.charOffset,
                                bookmark.note)).execute();
                SyncOutcome outcome = SyncOutcome.withAttempts(
                        SyncOutcome.fromHttpCode(response.code()), getRunAttemptCount());
                if (outcome == SyncOutcome.RETRY) return Result.retry();
                if (outcome == SyncOutcome.STOP_AUTH) return Result.failure();
                if (outcome == SyncOutcome.DROP) {
                    SyncErrorStore.record(context, "Không đồng bộ được dấu trang chương "
                            + bookmark.chapterNo + ": "
                            + ApiErrorParser.getMessage(response, "server từ chối"));
                }
                // xoá ngay sau khi server trả lời để tránh gửi trùng
                dao.deletePendingBookmark(bookmark.localId);
            }
            return Result.success();
        } catch (IOException exception) {
            return Result.retry();
        }
    }
}
