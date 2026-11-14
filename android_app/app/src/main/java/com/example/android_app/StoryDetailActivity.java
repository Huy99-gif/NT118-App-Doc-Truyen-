package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.android_app.adapter.ChapterAdapter;
import com.example.android_app.databinding.ActivityStoryDetailBinding;
import com.example.android_app.model.Chapter;
import com.example.android_app.model.ChapterSummaryResponse;
import com.example.android_app.model.LibraryItemResponse;
import com.example.android_app.model.PageResponse;
import com.example.android_app.model.ReadingHistoryResponse;
import com.example.android_app.model.StoryDetailResponse;
import com.example.android_app.network.ApiClient;
import com.example.android_app.util.ApiErrorParser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoryDetailActivity extends AppCompatActivity {
    private ActivityStoryDetailBinding binding;
    private ChapterAdapter chapterAdapter;
    private final List<ChapterSummaryResponse> chapters = new ArrayList<>();
    private long storyId;
    private String storyName;
    private String storyAuthor;
    private String storyCover;
    private int chapterCount;
    private boolean isSaved;
    private ReadingHistoryResponse readingHistory;

    private Call<StoryDetailResponse> detailCall;
    private Call<PageResponse<ChapterSummaryResponse>> chaptersCall;
    private Call<PageResponse<LibraryItemResponse>> libraryCall;
    private Call<PageResponse<ReadingHistoryResponse>> historyCall;
    private Call<Void> libraryMutationCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStoryDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getIntentData();
        setupUI();
        setupListeners();
        loadStoryDetail();
        loadChapters();
        loadLibraryStatus();
        loadReadingHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReadingHistory();
    }

    @Override
    protected void onDestroy() {
        cancel(detailCall);
        cancel(chaptersCall);
        cancel(libraryCall);
        cancel(historyCall);
        cancel(libraryMutationCall);
        super.onDestroy();
    }

    private void getIntentData() {
        Object rawStoryId = getIntent().getExtras() == null
                ? null : getIntent().getExtras().get("story_id");
        if (rawStoryId instanceof Number) {
            storyId = ((Number) rawStoryId).longValue();
        } else if (rawStoryId instanceof String) {
            try {
                storyId = Long.parseLong((String) rawStoryId);
            } catch (NumberFormatException ignored) {
                storyId = 0L;
            }
        }
        storyName = getIntent().getStringExtra("story_name");
        storyAuthor = getIntent().getStringExtra("story_author");
        storyCover = getIntent().getStringExtra("story_cover");
    }

    private void setupUI() {
        binding.tvStoryName.setText(storyName == null ? "Đang tải truyện..." : storyName);
        binding.tvAuthor.setText("Tác giả: " + (storyAuthor == null ? "Đang cập nhật" : storyAuthor));
        binding.tvStatus.setText("Trạng thái: Đang tải");
        binding.tvCategory.setText("Thể loại: Đang tải");
        binding.tvDescription.setText("Đang tải nội dung giới thiệu...");
        Glide.with(this)
                .load(storyCover)
                .apply(new RequestOptions()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivCover);

        chapterAdapter = new ChapterAdapter(chapter -> openReader(Long.parseLong(chapter.getId()), chapter.getChapterNumber(), 0));
        binding.rvChapters.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChapters.setAdapter(chapterAdapter);
        updateReadButton();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnReadFirst.setOnClickListener(v -> {
            if (readingHistory != null && readingHistory.chapterId != null && readingHistory.chapterId > 0) {
                int chNo = readingHistory.chapterNo == null ? chapterNumber(readingHistory.chapterId) : readingHistory.chapterNo;
                int offset = readingHistory.charOffset == null ? 0 : readingHistory.charOffset;
                openReader(readingHistory.chapterId, chNo, offset);
            } else if (!chapters.isEmpty()) {
                openReader(chapters.get(0).id, 1, 0);
            }
        });
        binding.btnBookmark.setOnClickListener(v -> changeLibraryMembership());
    }

    private void loadStoryDetail() {
        String authorization = requireAuthorization(false);
        if (authorization == null || storyId <= 0) return;

        detailCall = ApiClient.api().getStory(authorization, storyId);
        detailCall.enqueue(new Callback<StoryDetailResponse>() {
            @Override
            public void onResponse(Call<StoryDetailResponse> call,
                                   Response<StoryDetailResponse> response) {
                if (isFinishing() || call.isCanceled()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    showError(response, "Không tải được thông tin truyện.");
                    return;
                }
                StoryDetailResponse story = response.body();
                storyName = story.title;
                storyAuthor = story.authorName;
                storyCover = story.coverUrl;
                binding.tvStoryName.setText(story.title);
                binding.tvAuthor.setText("Tác giả: " + valueOrDefault(story.authorName, "Đang cập nhật"));
                binding.tvStatus.setText("Trạng thái: " + statusLabel(story.status));
                binding.tvCategory.setText("Thể loại: "
                        + (story.categories == null || story.categories.isEmpty()
                        ? "Chưa phân loại" : android.text.TextUtils.join(", ", story.categories)));
                binding.tvDescription.setText(valueOrDefault(story.summary, "Chưa có giới thiệu."));
                Glide.with(StoryDetailActivity.this)
                        .load(story.coverUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .error(R.drawable.ic_launcher_foreground))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.ivCover);
            }

            @Override
            public void onFailure(Call<StoryDetailResponse> call, Throwable throwable) {
                if (!isFinishing() && !call.isCanceled()) {
                    Toast.makeText(StoryDetailActivity.this,
                            R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadChapters() {
        String authorization = requireAuthorization(false);
        if (authorization == null || storyId <= 0) return;

        binding.progressBarChapters.setVisibility(View.VISIBLE);
        chaptersCall = ApiClient.api().getStoryChapters(authorization, storyId, 0, 50);
        chaptersCall.enqueue(new Callback<PageResponse<ChapterSummaryResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<ChapterSummaryResponse>> call,
                                   Response<PageResponse<ChapterSummaryResponse>> response) {
                if (isFinishing() || call.isCanceled()) return;
                binding.progressBarChapters.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null) {
                    showError(response, "Không tải được danh sách chương.");
                    return;
                }
                chapters.clear();
                chapters.addAll(response.body().items);
                chapterCount = (int) Math.min(Integer.MAX_VALUE, response.body().totalElements);
                List<Chapter> adapterItems = new ArrayList<>();
                for (ChapterSummaryResponse item : chapters) {
                    adapterItems.add(new Chapter(String.valueOf(item.id),
                            item.chapterNo == null ? 0 : item.chapterNo,
                            item.title, item.updatedAt, 0));
                }
                chapterAdapter.setChapters(adapterItems);
                binding.tvChapterHeader.setText("Danh sách chương (" + chapterCount + " chương)");
                updateReadButton();
                if (chapters.isEmpty()) {
                    Toast.makeText(StoryDetailActivity.this,
                            "Truyện chưa có chương.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<ChapterSummaryResponse>> call, Throwable throwable) {
                if (isFinishing() || call.isCanceled()) return;
                binding.progressBarChapters.setVisibility(View.GONE);
                Toast.makeText(StoryDetailActivity.this,
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadLibraryStatus() {
        String authorization = requireAuthorization(false);
        if (authorization == null) return;
        libraryCall = ApiClient.api().getLibrary(authorization, 0, 50);
        libraryCall.enqueue(new Callback<PageResponse<LibraryItemResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<LibraryItemResponse>> call,
                                   Response<PageResponse<LibraryItemResponse>> response) {
                if (isFinishing() || call.isCanceled()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    showError(response, "Không kiểm tra được tủ sách.");
                    return;
                }
                isSaved = false;
                for (LibraryItemResponse item : response.body().items) {
                    if (item.storyId != null && item.storyId == storyId) {
                        isSaved = true;
                        break;
                    }
                }
                updateLibraryButton();
            }

            @Override
            public void onFailure(Call<PageResponse<LibraryItemResponse>> call, Throwable throwable) {
                if (!isFinishing() && !call.isCanceled()) {
                    Toast.makeText(StoryDetailActivity.this,
                            "Không kiểm tra được trạng thái tủ sách.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadReadingHistory() {
        String authorization = requireAuthorization(false);
        if (authorization == null || storyId <= 0) return;
        if (historyCall != null) historyCall.cancel();
        historyCall = ApiClient.api().getReadingHistory(authorization, 0, 50);
        historyCall.enqueue(new Callback<PageResponse<ReadingHistoryResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<ReadingHistoryResponse>> call,
                                   Response<PageResponse<ReadingHistoryResponse>> response) {
                if (isFinishing() || call.isCanceled()) return;
                if (!response.isSuccessful() || response.body() == null) return;
                readingHistory = null;
                for (ReadingHistoryResponse item : response.body().items) {
                    if (item.storyId != null && item.storyId == storyId) {
                        readingHistory = item;
                        break;
                    }
                }
                updateReadButton();
            }

            @Override
            public void onFailure(Call<PageResponse<ReadingHistoryResponse>> call, Throwable throwable) {
                // Background check failed, fallback to default
            }
        });
    }

    private void changeLibraryMembership() {
        String authorization = requireAuthorization(true);
        if (authorization == null || storyId <= 0) return;
        binding.btnBookmark.setEnabled(false);
        libraryMutationCall = isSaved
                ? ApiClient.api().removeFromLibrary(authorization, storyId)
                : ApiClient.api().addToLibrary(authorization,
                        new com.example.android_app.model.AddLibraryRequest(storyId));
        libraryMutationCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (isFinishing() || call.isCanceled()) return;
                binding.btnBookmark.setEnabled(true);
                if (!response.isSuccessful()) {
                    Toast.makeText(StoryDetailActivity.this,
                            ApiErrorParser.getMessage(response, "Không cập nhật được tủ sách."),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                isSaved = !isSaved;
                updateLibraryButton();
                Toast.makeText(StoryDetailActivity.this,
                        isSaved ? "Đã thêm vào tủ sách." : "Đã bỏ khỏi tủ sách.",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                if (isFinishing() || call.isCanceled()) return;
                binding.btnBookmark.setEnabled(true);
                Toast.makeText(StoryDetailActivity.this,
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateLibraryButton() {
        binding.btnBookmark.setText(isSaved ? "Đã lưu tủ sách" : "Lưu tủ sách");
    }

    private void updateReadButton() {
        if (readingHistory != null && readingHistory.chapterId != null && readingHistory.chapterId > 0) {
            int chNo = readingHistory.chapterNo == null ? 1 : readingHistory.chapterNo;
            binding.btnReadFirst.setText("Đọc tiếp (Chương " + chNo + ")");
            binding.btnReadFirst.setEnabled(true);
        } else {
            binding.btnReadFirst.setText("Đọc từ đầu");
            binding.btnReadFirst.setEnabled(!chapters.isEmpty());
        }
    }

    private void openReader(long chapterId, int chapterNo, int charOffset) {
        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra("story_id", storyId);
        intent.putExtra("story_name", storyName);
        intent.putExtra("chapter_id", chapterId);
        intent.putExtra("chapter_number", chapterNo);
        intent.putExtra("char_offset", charOffset);
        intent.putExtra("total_chapters", chapterCount);
        long[] chapterIds = new long[chapters.size()];
        int[] chapterNumbers = new int[chapters.size()];
        String[] chapterTitles = new String[chapters.size()];
        for (int i = 0; i < chapters.size(); i++) {
            ChapterSummaryResponse chapter = chapters.get(i);
            chapterIds[i] = chapter.id;
            chapterNumbers[i] = chapter.chapterNo == null ? i + 1 : chapter.chapterNo;
            chapterTitles[i] = chapter.title;
        }
        intent.putExtra("chapter_ids", chapterIds);
        intent.putExtra("chapter_numbers", chapterNumbers);
        intent.putExtra("chapter_titles", chapterTitles);
        startActivity(intent);
    }

    private int chapterNumber(long chapterId) {
        for (int i = 0; i < chapters.size(); i++) {
            if (chapters.get(i).id != null && chapters.get(i).id == chapterId) {
                return chapters.get(i).chapterNo == null ? i + 1 : chapters.get(i).chapterNo;
            }
        }
        return 1;
    }

    private String requireAuthorization(boolean showToast) {
        String authorization = ApiClient.authorization(this);
        if (authorization == null && showToast) {
            Toast.makeText(this, "Vui lòng đăng nhập để sử dụng chức năng này.",
                    Toast.LENGTH_LONG).show();
        }
        return authorization;
    }

    private void showError(Response<?> response, String fallback) {
        Toast.makeText(this, ApiErrorParser.getMessage(response, fallback), Toast.LENGTH_LONG).show();
    }

    private String statusLabel(String status) {
        if ("ONGOING".equals(status)) return "Đang ra";
        if ("COMPLETED".equals(status)) return "Hoàn thành";
        return valueOrDefault(status, "Đang cập nhật");
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private void cancel(Call<?> call) {
        if (call != null) call.cancel();
    }
}
