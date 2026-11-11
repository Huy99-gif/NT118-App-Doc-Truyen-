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
import com.example.android_app.util.ApiErrorParser;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReaderActivity extends AppCompatActivity {
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
        binding.scrollView.setOnScrollChangeListener(
                (androidx.core.widget.NestedScrollView view, int scrollX, int scrollY,
                 int oldScrollX, int oldScrollY) -> {
                    if (chapterId > 0) {
                        handler.removeCallbacks(saveProgressTask);
                        handler.postDelayed(saveProgressTask, 1200);
                    }
                });
    }

    private void loadChapterContent(long id, int chapterNumber, String fallbackTitle) {
        String authorization = ApiClient.authorization(this);
        if (authorization == null || id <= 0) {
            Toast.makeText(this, "Vui lòng đăng nhập để đọc chương.", Toast.LENGTH_LONG).show();
            return;
        }
        chapterId = id;
        currentChapterNumber = chapterNumber;
        chapterTitle = fallbackTitle;
        binding.progressBarReader.setVisibility(View.VISIBLE);
        binding.tvChapterContent.setVisibility(View.GONE);
        binding.scrollView.scrollTo(0, 0);
        updateNavigationButtons();
        if (chapterCall != null) chapterCall.cancel();
        chapterCall = ApiClient.api().getChapter(authorization, id);
        chapterCall.enqueue(new Callback<ChapterDetailResponse>() {
            @Override
            public void onResponse(Call<ChapterDetailResponse> call,
                                   Response<ChapterDetailResponse> response) {
                if (isFinishing() || call.isCanceled()) return;
                binding.progressBarReader.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ReaderActivity.this,
                            ApiErrorParser.getMessage(response, "Không tải được nội dung chương."),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                ChapterDetailResponse chapter = response.body();
                chapterId = chapter.id == null ? id : chapter.id;
                storyId = chapter.storyId == null ? storyId : chapter.storyId;
                currentChapterNumber = chapter.chapterNo == null
                        ? chapterNumber : chapter.chapterNo;
                chapterTitle = chapter.title;
                binding.tvToolbarTitle.setText(displayStoryName() + " - Chương " + currentChapterNumber);
                binding.tvChapterTitle.setText("Chương " + currentChapterNumber + ": "
                        + (chapterTitle == null ? "" : chapterTitle));
                binding.tvChapterInfo.setText(chapter.updatedAt == null
                        ? "" : "Cập nhật: " + chapter.updatedAt);
                binding.btnChapterList.setText("Chương " + currentChapterNumber);
                binding.tvChapterContent.setText(chapter.content == null ? "" : chapter.content);
                binding.tvChapterContent.setVisibility(View.VISIBLE);
                updateNavigationButtons();
                if (restoreInitialOffset) {
                    restoreInitialOffset = false;
                    restoreOffset(initialCharOffset);
                }
                saveReadingProgress();
            }

            @Override
            public void onFailure(Call<ChapterDetailResponse> call, Throwable throwable) {
                if (isFinishing() || call.isCanceled()) return;
                binding.progressBarReader.setVisibility(View.GONE);
                Toast.makeText(ReaderActivity.this,
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadChapterListIfNeeded() {
        if (chapterIds.length > 0 || storyId <= 0) return;
        String authorization = ApiClient.authorization(this);
        if (authorization == null) return;
        chaptersCall = ApiClient.api().getStoryChapters(authorization, storyId, 0, 50);
        chaptersCall.enqueue(new Callback<PageResponse<ChapterSummaryResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<ChapterSummaryResponse>> call,
                                   Response<PageResponse<ChapterSummaryResponse>> response) {
                if (isFinishing() || call.isCanceled() || !response.isSuccessful()
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
                if (!call.isCanceled()) Log.w(TAG, "Không tải được danh sách chương để điều hướng.", throwable);
            }
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

    private void showCreateBookmarkDialog() {
        if (chapterId <= 0) return;
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
        String authorization = ApiClient.authorization(this);
        if (authorization == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để tạo dấu trang.", Toast.LENGTH_LONG).show();
            return;
        }
        int offset = currentCharOffset();
        bookmarkCall = ApiClient.api().createBookmark(authorization,
                new CreateBookmarkRequest(chapterId, offset, note));
        bookmarkCall.enqueue(new Callback<BookmarkResponse>() {
            @Override
            public void onResponse(Call<BookmarkResponse> call, Response<BookmarkResponse> response) {
                if (isFinishing() || call.isCanceled()) return;
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
                if (!isFinishing() && !call.isCanceled()) {
                    Toast.makeText(ReaderActivity.this, R.string.error_server_unreachable,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveReadingProgress() {
        String authorization = ApiClient.authorization(this);
        if (authorization == null || storyId <= 0 || chapterId <= 0
                || binding.tvChapterContent.getVisibility() != View.VISIBLE) return;
        if (historyRequestInFlight) {
            historyUpdatePending = true;
            return;
        }
        historyRequestInFlight = true;
        historyCall = ApiClient.api().updateReadingHistory(authorization,
                new UpdateReadingHistoryRequest(storyId, chapterId, currentCharOffset()));
        historyCall.enqueue(new Callback<ReadingHistoryResponse>() {
            @Override
            public void onResponse(Call<ReadingHistoryResponse> call,
                                   Response<ReadingHistoryResponse> response) {
                if (!response.isSuccessful() && !call.isCanceled()) {
                    Log.w(TAG, "Lưu lịch sử đọc thất bại: HTTP " + response.code());
                }
                finishHistorySave();
            }

            @Override
            public void onFailure(Call<ReadingHistoryResponse> call, Throwable throwable) {
                if (!call.isCanceled()) Log.w(TAG, "Không lưu được lịch sử đọc.", throwable);
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

    private void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }
}
