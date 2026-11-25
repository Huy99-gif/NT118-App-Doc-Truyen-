package com.example.android_app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android_app.databinding.ActivityReaderBinding;
import com.example.android_app.model.BookmarkResponse;
import com.example.android_app.model.ChapterDetailResponse;
import com.example.android_app.model.ChapterSummaryResponse;
import com.example.android_app.model.CreateBookmarkRequest;
import com.example.android_app.model.PageResponse;
import com.example.android_app.model.ReadingHistoryResponse;
import com.example.android_app.model.UpdateReadingHistoryRequest;
import com.example.android_app.network.ApiClient;
import com.example.android_app.offline.OfflineExecutors;
import com.example.android_app.offline.db.ChapterHeader;
import com.example.android_app.offline.db.DownloadedChapterEntity;
import com.example.android_app.offline.db.LocalProgressEntity;
import com.example.android_app.offline.db.OfflineDao;
import com.example.android_app.offline.db.OfflineDatabase;
import com.example.android_app.offline.db.PendingBookmarkEntity;
import com.example.android_app.offline.db.PendingProgressEntity;
import com.example.android_app.offline.work.OfflineWork;
import com.example.android_app.util.ApiErrorParser;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReaderActivity extends AppCompatActivity {
    /** Mở từ "Đọc truyện đã tải" khi không còn phiên: chỉ đọc bản trong máy, không gọi API. */
    public static final String EXTRA_OFFLINE_MODE = "offline_mode";

    private static final String TAG = "ReaderActivity";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable saveProgressTask = this::saveReadingProgress;
    private ActivityReaderBinding binding;
    private long storyId;
    private String storyName;
    private long chapterId;
    private int currentChapterNumber = 1;
    private int totalChapters;
    private String chapterTitle;
    private int initialCharOffset;
    private boolean restoreInitialOffset;
    private boolean historyRequestInFlight;
    private boolean historyUpdatePending;
    private String ownerKey;
    private boolean offlineMode;
    private boolean networkUnavailable;
    private boolean contentLoaded;
    private long[] chapterIds = new long[0];
    private int[] chapterNumbers = new int[0];
    private String[] chapterTitles = new String[0];
    private Call<ChapterDetailResponse> chapterCall;
    private Call<PageResponse<ChapterSummaryResponse>> chaptersCall;
    private Call<ReadingHistoryResponse> historyCall;
    private Call<BookmarkResponse> bookmarkCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getIntentData();
        setupListeners();
        loadChapterListIfNeeded();
        loadChapterContent(chapterId, currentChapterNumber, chapterTitle);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(saveProgressTask);
        saveReadingProgress();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(saveProgressTask);
        cancel(chapterCall);
        cancel(chaptersCall);
        cancel(bookmarkCall);
        cancel(historyCall);
        super.onDestroy();
    }

    private void getIntentData() {
        storyId = getIntent().getLongExtra("story_id", 0L);
        chapterId = getIntent().getLongExtra("chapter_id", 0L);
        storyName = getIntent().getStringExtra("story_name");
        currentChapterNumber = getIntent().getIntExtra("chapter_number", 1);
        totalChapters = getIntent().getIntExtra("total_chapters", 0);
        chapterTitle = getIntent().getStringExtra("chapter_title");
        initialCharOffset = Math.max(0, getIntent().getIntExtra("char_offset", 0));
        restoreInitialOffset = initialCharOffset > 0;
        offlineMode = getIntent().getBooleanExtra(EXTRA_OFFLINE_MODE, false);
        ownerKey = AuthPreferences.getOwnerKey(this);
        long[] incomingIds = getIntent().getLongArrayExtra("chapter_ids");
        int[] incomingNumbers = getIntent().getIntArrayExtra("chapter_numbers");
        String[] incomingTitles = getIntent().getStringArrayExtra("chapter_titles");
        if (incomingIds != null && incomingNumbers != null && incomingTitles != null
                && incomingIds.length == incomingNumbers.length
                && incomingIds.length == incomingTitles.length) {
            chapterIds = incomingIds;
            chapterNumbers = incomingNumbers;
            chapterTitles = incomingTitles;
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnPrevChapter.setOnClickListener(v -> navigateChapter(-1));
        binding.btnNextChapter.setOnClickListener(v -> navigateChapter(1));
        binding.btnChapterList.setOnClickListener(v -> showChapterList());
        binding.btnAddBookmark.setOnClickListener(v -> showCreateBookmarkDialog());
        binding.btnComments.setOnClickListener(v -> showComments());
        binding.scrollView.setOnScrollChangeListener(
                (androidx.core.widget.NestedScrollView view, int scrollX, int scrollY,
                 int oldScrollX, int oldScrollY) -> {
                    if (chapterId > 0) {
                        // debounce 1.2s ngưng cuộn mới lưu vị trí đọc tránh spam api
                        handler.removeCallbacks(saveProgressTask);
                        handler.postDelayed(saveProgressTask, 1200);
                    }
                });
    }

    private OfflineDao dao() {
        return OfflineDatabase.get(this).dao();
    }

    private void loadChapterContent(long id, int chapterNumber, String fallbackTitle) {
        if (id <= 0) return;
        chapterId = id;
        currentChapterNumber = chapterNumber;
        chapterTitle = fallbackTitle;
        contentLoaded = false;
        binding.progressBarReader.setVisibility(View.VISIBLE);
        binding.tvChapterContent.setVisibility(View.GONE);
        binding.scrollView.scrollTo(0, 0);
        updateNavigationButtons();
        if (chapterCall != null) chapterCall.cancel();

        String owner = ownerKey;
        if (owner == null) {
            loadChapterFromNetwork(id, chapterNumber);
            return;
        }
        // ưu tiên bản đã tải trong máy: mở ngay, không cần mạng
        OfflineExecutors.io().execute(() -> {
            DownloadedChapterEntity local = dao().findChapter(owner, id);
            OfflineExecutors.main(() -> {
                if (isFinishing() || isDestroyed() || chapterId != id) return;
                if (local != null) {
                    binding.progressBarReader.setVisibility(View.GONE);
                    renderChapter(local.chapterId, local.storyId, local.chapterNo,
                            local.title, local.content, local.updatedAt);
                } else {
                    loadChapterFromNetwork(id, chapterNumber);
                }
            });
        });
    }

    private void loadChapterFromNetwork(long id, int chapterNumber) {
        if (offlineMode) {
            showChapterNotDownloaded();
            return;
        }
        String authorization = ApiClient.authorization(this);
        if (authorization == null) {
            binding.progressBarReader.setVisibility(View.GONE);
            Toast.makeText(this, "Vui lòng đăng nhập để đọc chương.", Toast.LENGTH_LONG).show();
            return;
        }
        chapterCall = ApiClient.api().getChapter(authorization, id);
        chapterCall.enqueue(new Callback<ChapterDetailResponse>() {
            @Override
            public void onResponse(Call<ChapterDetailResponse> call,
                                   Response<ChapterDetailResponse> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                binding.progressBarReader.setVisibility(View.GONE);
                networkUnavailable = false;
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ReaderActivity.this,
                            ApiErrorParser.getMessage(response, "Không tải được nội dung chương."),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                ChapterDetailResponse chapter = response.body();
                renderChapter(chapter.id == null ? id : chapter.id,
                        chapter.storyId == null ? storyId : chapter.storyId,
                        chapter.chapterNo == null ? chapterNumber : chapter.chapterNo,
                        chapter.title, chapter.content, chapter.updatedAt);
            }

            @Override
            public void onFailure(Call<ChapterDetailResponse> call, Throwable throwable) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                if (throwable instanceof IOException) {
                    networkUnavailable = true;
                    showChapterNotDownloaded();
                    return;
                }
                binding.progressBarReader.setVisibility(View.GONE);
                Toast.makeText(ReaderActivity.this,
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderChapter(long id, long chapterStoryId, int chapterNo, String title,
                               String content, String updatedAt) {
        chapterId = id;
        storyId = chapterStoryId > 0 ? chapterStoryId : storyId;
        currentChapterNumber = chapterNo;
        chapterTitle = title;
        binding.tvToolbarTitle.setText(displayStoryName() + " - Chương " + currentChapterNumber);
        binding.tvChapterTitle.setText("Chương " + currentChapterNumber + ": "
                + (chapterTitle == null ? "" : chapterTitle));
        binding.tvChapterInfo.setText(formatUpdatedDate(updatedAt));
        binding.btnChapterList.setText("Chương " + currentChapterNumber);
        binding.tvChapterContent.setText(content == null ? "" : content);
        binding.tvChapterContent.setVisibility(View.VISIBLE);
        contentLoaded = true;
        updateNavigationButtons();
        if (restoreInitialOffset) {
            restoreInitialOffset = false;
            restoreOffset(initialCharOffset);
        }
        saveReadingProgress();
    }

    private void showChapterNotDownloaded() {
        contentLoaded = false;
        binding.progressBarReader.setVisibility(View.GONE);
        binding.tvChapterTitle.setText("Chương " + currentChapterNumber + ": "
                + (chapterTitle == null ? "" : chapterTitle));
        binding.btnChapterList.setText("Chương " + currentChapterNumber);
        binding.tvChapterInfo.setText("");
        binding.tvChapterContent.setText(R.string.offline_chapter_not_downloaded);
        binding.tvChapterContent.setVisibility(View.VISIBLE);
        updateNavigationButtons();
    }

    private void loadChapterListIfNeeded() {
        if (chapterIds.length > 0 || storyId <= 0) return;
        String authorization = ApiClient.authorization(this);
        if (offlineMode || authorization == null) {
            loadChapterListFromDevice();
            return;
        }
        chaptersCall = ApiClient.api().getStoryChapters(authorization, storyId, 0, 50);
        chaptersCall.enqueue(new Callback<PageResponse<ChapterSummaryResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<ChapterSummaryResponse>> call,
                                   Response<PageResponse<ChapterSummaryResponse>> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled() || !response.isSuccessful()
                        || response.body() == null) return;
                List<ChapterSummaryResponse> items = response.body().items;
                chapterIds = new long[items.size()];
                chapterNumbers = new int[items.size()];
                chapterTitles = new String[items.size()];
                totalChapters = (int) Math.min(Integer.MAX_VALUE, response.body().totalElements);
                for (int i = 0; i < items.size(); i++) {
                    ChapterSummaryResponse item = items.get(i);
                    chapterIds[i] = item.id == null ? 0 : item.id;
                    chapterNumbers[i] = item.chapterNo == null ? i + 1 : item.chapterNo;
                    chapterTitles[i] = item.title;
                }
                updateNavigationButtons();
            }

            @Override
            public void onFailure(Call<PageResponse<ChapterSummaryResponse>> call, Throwable throwable) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                Log.w(TAG, "Không tải được danh sách chương để điều hướng.", throwable);
                if (throwable instanceof IOException) {
                    networkUnavailable = true;
                    loadChapterListFromDevice();
                }
            }
        });
    }

    // offline: chuyển trước/sau giữa các chương đã tải của truyện
    private void loadChapterListFromDevice() {
        String owner = ownerKey;
        long story = storyId;
        if (owner == null || story <= 0) return;
        OfflineExecutors.io().execute(() -> {
            List<ChapterHeader> headers = dao().chapterHeaders(owner, story);
            OfflineExecutors.main(() -> {
                if (isFinishing() || isDestroyed() || headers.isEmpty()) return;
                chapterIds = new long[headers.size()];
                chapterNumbers = new int[headers.size()];
                chapterTitles = new String[headers.size()];
                for (int i = 0; i < headers.size(); i++) {
                    chapterIds[i] = headers.get(i).chapterId;
                    chapterNumbers[i] = headers.get(i).chapterNo;
                    chapterTitles[i] = headers.get(i).title;
                }
                updateNavigationButtons();
            });
        });
    }

    private void navigateChapter(int direction) {
        int currentIndex = findCurrentChapterIndex();
        int nextIndex = currentIndex + direction;
        if (currentIndex < 0 || nextIndex < 0 || nextIndex >= chapterIds.length) {
            Toast.makeText(this,
                    direction < 0 ? "Đây là chương đầu tiên." : "Đây là chương mới nhất.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        loadChapterContent(chapterIds[nextIndex], chapterNumbers[nextIndex], chapterTitles[nextIndex]);
    }

    private void showChapterList() {
        if (chapterIds.length == 0) {
            Toast.makeText(this, "Danh sách chương chưa tải xong.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] labels = new String[chapterIds.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = "Chương " + chapterNumbers[i] + ": "
                    + (chapterTitles[i] == null ? "" : chapterTitles[i]);
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Danh sách chương")
                .setItems(labels, (dialog, which) ->
                        loadChapterContent(chapterIds[which], chapterNumbers[which], chapterTitles[which]))
                .show();
    }

    private void showComments() {
        if (offlineMode || networkUnavailable) {
            Toast.makeText(this, R.string.offline_comments_need_network, Toast.LENGTH_SHORT).show();
            return;
        }
        // đọc chapterId tại lúc bấm nên luôn là chương đang hiển thị; chặn mở 2 sheet khi bấm nhanh
        if (chapterId <= 0
                || getSupportFragmentManager().findFragmentByTag(CommentsBottomSheet.TAG) != null) {
            return;
        }
        CommentsBottomSheet.newInstance(chapterId, currentChapterNumber)
                .show(getSupportFragmentManager(), CommentsBottomSheet.TAG);
    }

    private void showCreateBookmarkDialog() {
        if (chapterId <= 0 || !contentLoaded) return;
        EditText noteInput = new EditText(this);
        noteInput.setSingleLine(true);
        noteInput.setText("Đọc lại");
        noteInput.setSelection(noteInput.getText().length());
        new MaterialAlertDialogBuilder(this)
                .setTitle("Tạo dấu trang")
                .setMessage("Ghi chú cho vị trí đọc hiện tại")
                .setView(noteInput)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", (dialog, which) ->
                        createBookmark(noteInput.getText().toString().trim()))
                .show();
    }

    private void createBookmark(String note) {
        int offset = currentCharOffset();
        String authorization = ApiClient.authorization(this);
        if (offlineMode || authorization == null) {
            queueBookmark(note, offset);
            return;
        }
        bookmarkCall = ApiClient.api().createBookmark(authorization,
                new CreateBookmarkRequest(chapterId, offset, note));
        bookmarkCall.enqueue(new Callback<BookmarkResponse>() {
            @Override
            public void onResponse(Call<BookmarkResponse> call, Response<BookmarkResponse> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ReaderActivity.this,
                            ApiErrorParser.getMessage(response, "Không tạo được dấu trang."),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(ReaderActivity.this, "Đã lưu dấu trang.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<BookmarkResponse> call, Throwable throwable) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                if (throwable instanceof IOException) {
                    networkUnavailable = true;
                    queueBookmark(note, offset);
                } else {
                    Toast.makeText(ReaderActivity.this, R.string.error_server_unreachable,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // offline: giữ dấu trang trong máy, gửi lên khi có mạng
    private void queueBookmark(String note, int offset) {
        String owner = ownerKey;
        if (owner == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để tạo dấu trang.", Toast.LENGTH_LONG).show();
            return;
        }
        PendingBookmarkEntity bookmark = new PendingBookmarkEntity();
        bookmark.ownerKey = owner;
        bookmark.storyId = storyId;
        bookmark.chapterId = chapterId;
        bookmark.chapterNo = currentChapterNumber;
        bookmark.chapterTitle = chapterTitle;
        bookmark.storyTitle = storyName;
        bookmark.charOffset = offset;
        bookmark.note = note;
        bookmark.createdAt = System.currentTimeMillis();
        OfflineExecutors.io().execute(() -> {
            dao().insertPendingBookmark(bookmark);
            OfflineWork.enqueueSync(getApplicationContext(), owner);
        });
        Toast.makeText(this, R.string.offline_bookmark_queued, Toast.LENGTH_SHORT).show();
    }

    private void saveReadingProgress() {
        if (storyId <= 0 || chapterId <= 0 || !contentLoaded) return;
        String owner = ownerKey;
        long story = storyId;
        long chapter = chapterId;
        int offset = currentCharOffset();
        long updatedAt = System.currentTimeMillis();
        if (owner != null) {
            // luôn ghi vào máy trước: local_progress để mở lại đúng chỗ, pending_progress để đồng bộ
            OfflineExecutors.io().execute(() -> {
                LocalProgressEntity local = new LocalProgressEntity();
                local.ownerKey = owner;
                local.storyId = story;
                local.chapterId = chapter;
                local.charOffset = offset;
                local.updatedAt = updatedAt;
                dao().upsertLocalProgress(local);
                PendingProgressEntity pending = new PendingProgressEntity();
                pending.ownerKey = owner;
                pending.storyId = story;
                pending.chapterId = chapter;
                pending.charOffset = offset;
                pending.updatedAt = updatedAt;
                dao().upsertPendingProgress(pending);
            });
        }

        String authorization = ApiClient.authorization(this);
        if (offlineMode || authorization == null) return;
        if (historyRequestInFlight) {
            historyUpdatePending = true;
            return;
        }
        historyRequestInFlight = true;
        historyCall = ApiClient.api().updateReadingHistory(authorization,
                new UpdateReadingHistoryRequest(story, chapter, offset));
        historyCall.enqueue(new Callback<ReadingHistoryResponse>() {
            @Override
            public void onResponse(Call<ReadingHistoryResponse> call,
                                   Response<ReadingHistoryResponse> response) {
                if (call.isCanceled()) return;
                if (response.isSuccessful()) {
                    if (owner != null) {
                        OfflineExecutors.io().execute(() ->
                                dao().deletePendingProgressIfUnchanged(owner, story, updatedAt));
                    }
                } else {
                    Log.w(TAG, "Lưu lịch sử đọc thất bại: HTTP " + response.code());
                    OfflineWork.enqueueSync(getApplicationContext(), owner);
                }
                if (isFinishing() || isDestroyed()) return;
                finishHistorySave();
            }

            @Override
            public void onFailure(Call<ReadingHistoryResponse> call, Throwable throwable) {
                if (call.isCanceled()) return;
                Log.w(TAG, "Không lưu được lịch sử đọc, sẽ đồng bộ khi có mạng.", throwable);
                OfflineWork.enqueueSync(getApplicationContext(), owner);
                if (isFinishing() || isDestroyed()) return;
                if (throwable instanceof IOException) networkUnavailable = true;
                finishHistorySave();
            }
        });
    }

    private void finishHistorySave() {
        historyRequestInFlight = false;
        if (historyUpdatePending) {
            historyUpdatePending = false;
            saveReadingProgress();
        }
    }

    private int currentCharOffset() {
        Layout textLayout = binding.tvChapterContent.getLayout();
        if (textLayout == null) return 0;
        int textTop = (int) binding.tvChapterContent.getY();
        int verticalOffset = Math.max(0, binding.scrollView.getScrollY() - textTop);
        int line = textLayout.getLineForVertical(verticalOffset);
        return Math.max(0, textLayout.getLineStart(line));
    }

    private void restoreOffset(int offset) {
        binding.tvChapterContent.post(() -> {
            Layout textLayout = binding.tvChapterContent.getLayout();
            if (textLayout == null || offset <= 0) return;
            int safeOffset = Math.min(offset, binding.tvChapterContent.length());
            int line = textLayout.getLineForOffset(safeOffset);
            binding.scrollView.scrollTo(0,
                    (int) binding.tvChapterContent.getY() + textLayout.getLineTop(line));
        });
    }

    private void updateNavigationButtons() {
        int currentIndex = findCurrentChapterIndex();
        binding.btnPrevChapter.setEnabled(currentIndex > 0);
        binding.btnNextChapter.setEnabled(currentIndex >= 0 && currentIndex < chapterIds.length - 1);
    }

    private int findCurrentChapterIndex() {
        for (int i = 0; i < chapterIds.length; i++) {
            if (chapterIds[i] == chapterId) return i;
        }
        return -1;
    }

    private String displayStoryName() {
        return storyName == null || storyName.trim().isEmpty() ? "Đọc truyện" : storyName;
    }

    private String formatUpdatedDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) return "";
        try {
            if (rawDate.contains("T")) {
                String datePart = rawDate.split("T")[0];
                String[] parts = datePart.split("-");
                if (parts.length == 3) {
                    return "Cập nhật: " + parts[2] + "/" + parts[1] + "/" + parts[0];
                }
            }
        } catch (Exception ignored) {}
        return "Cập nhật: " + rawDate;
    }

    private void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }
}
