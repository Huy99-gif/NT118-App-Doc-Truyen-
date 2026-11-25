package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.example.android_app.adapter.ChapterAdapter;
import com.example.android_app.databinding.ActivityStoryDetailBinding;
import com.example.android_app.model.Chapter;
import com.example.android_app.model.ChapterSummaryResponse;
import com.example.android_app.model.LibraryItemResponse;
import com.example.android_app.model.PageResponse;
import com.example.android_app.model.StoryDetailResponse;
import com.example.android_app.network.ApiClient;
import com.example.android_app.offline.OfflineExecutors;
import com.example.android_app.offline.db.ChapterHeader;
import com.example.android_app.offline.db.DownloadedStoryEntity;
import com.example.android_app.offline.db.OfflineDatabase;
import com.example.android_app.offline.work.OfflineWork;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.android_app.util.ApiErrorParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private String ownerKey;
    private boolean storyDownloadRunning;
    private final Set<Long> downloadedIds = new HashSet<>();
    private final Set<Long> queuedIds = new HashSet<>();
    private Call<StoryDetailResponse> detailCall;
    private Call<PageResponse<ChapterSummaryResponse>> chaptersCall;
    private Call<PageResponse<LibraryItemResponse>> libraryCall;
    private Call<Void> libraryMutationCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStoryDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getIntentData();
        ownerKey = AuthPreferences.getOwnerKey(this);
        setupUI();
        setupListeners();
        observeDownloads();
        loadStoryDetail();
        loadChapters();
        loadLibraryStatus();
    }

    @Override
    protected void onDestroy() {
        cancel(detailCall);
        cancel(chaptersCall);
        cancel(libraryCall);
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

        chapterAdapter = new ChapterAdapter(chapter -> openReader(Long.parseLong(chapter.getId())));
        chapterAdapter.setOnDownloadClickListener(this::onDownloadClick);
        chapterAdapter.setDownloadEnabled(ownerKey != null);
        binding.rvChapters.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChapters.setAdapter(chapterAdapter);
        binding.btnReadFirst.setEnabled(false);
    }

    private void observeDownloads() {
        if (ownerKey == null || storyId <= 0) return;
        OfflineDatabase.get(this).dao().observeChapterHeaders(ownerKey, storyId)
                .observe(this, headers -> {
                    downloadedIds.clear();
                    for (ChapterHeader header : headers) downloadedIds.add(header.chapterId);
                    refreshDownloadStates();
                });
        WorkManager workManager = WorkManager.getInstance(this);
        workManager.getWorkInfosByTagLiveData(OfflineWork.storyTag(ownerKey, storyId))
                .observe(this, infos -> {
                    queuedIds.clear();
                    for (WorkInfo info : infos) {
                        if (info.getState().isFinished()) continue;
                        for (String tag : info.getTags()) {
                            if (tag.startsWith("chapter-")) {
                                try {
                                    queuedIds.add(Long.parseLong(tag.substring("chapter-".length())));
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    refreshDownloadStates();
                });
        workManager.getWorkInfosForUniqueWorkLiveData(OfflineWork.storyWorkName(ownerKey, storyId))
                .observe(this, this::updateDownloadAllButton);
    }

    private void refreshDownloadStates() {
        Map<Long, ChapterAdapter.DownloadState> states = new HashMap<>();
        for (Long id : queuedIds) states.put(id, ChapterAdapter.DownloadState.QUEUED);
        for (Long id : downloadedIds) states.put(id, ChapterAdapter.DownloadState.DONE);
        chapterAdapter.setDownloadStates(states);
    }

    private void updateDownloadAllButton(List<WorkInfo> infos) {
        storyDownloadRunning = false;
        for (WorkInfo info : infos) {
            if (info.getState().isFinished()) continue;
            storyDownloadRunning = true;
            int done = info.getProgress().getInt(OfflineWork.PROGRESS_DONE, 0);
            int total = info.getProgress().getInt(OfflineWork.PROGRESS_TOTAL, chapterCount);
            binding.btnDownloadAll.setText("Huỷ tải (" + done + "/" + total + ")");
        }
        if (!storyDownloadRunning) binding.btnDownloadAll.setText("Tải cả truyện");
    }

    private void onDownloadClick(Chapter chapter, ChapterAdapter.DownloadState state) {
        if (ownerKey == null) {
            Toast.makeText(this, R.string.offline_need_owner, Toast.LENGTH_LONG).show();
            return;
        }
        long chapterId = Long.parseLong(chapter.getId());
        if (state == ChapterAdapter.DownloadState.NONE) {
            OfflineWork.enqueueChapterDownload(this, ownerKey, storyMeta(), chapterId);
        } else if (state == ChapterAdapter.DownloadState.DONE) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Xoá chương đã tải?")
                    .setMessage("Chương " + chapter.getChapterNumber() + " sẽ bị xoá khỏi máy.")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Xoá", (dialog, which) -> {
                        String owner = ownerKey;
                        long story = storyId;
                        OfflineExecutors.io().execute(() -> OfflineDatabase.get(this).dao()
                                .deleteChapter(owner, story, chapterId));
                    })
                    .show();
        }
    }

    private void onDownloadAllClick() {
        if (ownerKey == null) {
            Toast.makeText(this, R.string.offline_need_owner, Toast.LENGTH_LONG).show();
            return;
        }
        if (storyDownloadRunning) {
            OfflineWork.cancelStoryDownload(this, ownerKey, storyId);
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Tải cả truyện")
                .setMessage("Tải " + chapterCount + " chương để đọc khi không có mạng?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tải", (dialog, which) ->
                        OfflineWork.enqueueStoryDownload(this, ownerKey, storyMeta()))
                .show();
    }

    private DownloadedStoryEntity storyMeta() {
        DownloadedStoryEntity story = new DownloadedStoryEntity();
        story.ownerKey = ownerKey;
        story.storyId = storyId;
        story.title = storyName;
        story.authorName = storyAuthor;
        story.coverUrl = storyCover;
        story.totalChapters = chapterCount;
        return story;
    }

    // mất mạng: nếu truyện đã tải thì hiện thông tin lưu trong máy
    private void loadStoryFromDevice() {
        if (ownerKey == null) return;
        String owner = ownerKey;
        long id = storyId;
        OfflineExecutors.io().execute(() -> {
            DownloadedStoryEntity story = OfflineDatabase.get(this).dao().findStory(owner, id);
            OfflineExecutors.main(() -> {
                if (story == null || isFinishing() || isDestroyed()) return;
                storyName = story.title;
                storyAuthor = story.authorName;
                storyCover = story.coverUrl;
                binding.tvStoryName.setText(valueOrDefault(story.title, "Truyện đã tải"));
                binding.tvAuthor.setText("Tác giả: " + valueOrDefault(story.authorName, "Đang cập nhật"));
                binding.tvStatus.setText("Trạng thái: Đang offline");
                binding.tvCategory.setText("Thể loại: Không có kết nối mạng");
                binding.tvDescription.setText("Đang xem bản đã tải về máy.");
                Glide.with(StoryDetailActivity.this)
                        .load(story.coverUrl)
                        .apply(new RequestOptions()
                                .placeholder(R.drawable.ic_launcher_foreground)
                                .error(R.drawable.ic_launcher_foreground))
                        .into(binding.ivCover);
            });
        });
    }

    private void loadChaptersFromDevice() {
        if (ownerKey == null) {
            Toast.makeText(this, R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            return;
        }
        String owner = ownerKey;
        long id = storyId;
        OfflineExecutors.io().execute(() -> {
            List<ChapterHeader> headers = OfflineDatabase.get(this).dao().chapterHeaders(owner, id);
            OfflineExecutors.main(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (headers.isEmpty()) {
                    Toast.makeText(this, R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
                    return;
                }
                chapters.clear();
                List<Chapter> adapterItems = new ArrayList<>();
                for (ChapterHeader header : headers) {
                    ChapterSummaryResponse item = new ChapterSummaryResponse();
                    item.id = header.chapterId;
                    item.chapterNo = header.chapterNo;
                    item.title = header.title;
                    item.updatedAt = header.updatedAt;
                    chapters.add(item);
                    adapterItems.add(new Chapter(String.valueOf(header.chapterId), header.chapterNo,
                            header.title, header.updatedAt, 0));
                }
                chapterAdapter.setChapters(adapterItems);
                binding.tvChapterHeader.setText("Chương đã tải (" + headers.size() + " chương)");
                binding.btnReadFirst.setEnabled(true);
            });
        });
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnReadFirst.setOnClickListener(v -> {
            if (!chapters.isEmpty()) {
                // đọc chương đầu tiên trong danh sách
                openReader(chapters.get(0).id);
            }
        });
        binding.btnBookmark.setOnClickListener(v -> changeLibraryMembership());
        binding.btnDownloadAll.setOnClickListener(v -> onDownloadAllClick());
    }

    private void loadStoryDetail() {
        String authorization = requireAuthorization();
        if (authorization == null || storyId <= 0) return;

        detailCall = ApiClient.api().getStory(authorization, storyId);
        detailCall.enqueue(new Callback<StoryDetailResponse>() {
            @Override
            public void onResponse(Call<StoryDetailResponse> call,
                                   Response<StoryDetailResponse> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
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
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                if (throwable instanceof IOException) {
                    loadStoryFromDevice();
                } else {
                    Toast.makeText(StoryDetailActivity.this,
                            R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadChapters() {
        String authorization = requireAuthorization();
        if (authorization == null || storyId <= 0) return;

        binding.progressBarChapters.setVisibility(View.VISIBLE);
        chaptersCall = ApiClient.api().getStoryChapters(authorization, storyId, 0, 50);
        chaptersCall.enqueue(new Callback<PageResponse<ChapterSummaryResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<ChapterSummaryResponse>> call,
                                   Response<PageResponse<ChapterSummaryResponse>> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
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
                binding.btnReadFirst.setEnabled(!chapters.isEmpty());
                binding.btnDownloadAll.setEnabled(!chapters.isEmpty());
                if (chapters.isEmpty()) {
                    Toast.makeText(StoryDetailActivity.this,
                            "Truyện chưa có chương.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PageResponse<ChapterSummaryResponse>> call, Throwable throwable) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                binding.progressBarChapters.setVisibility(View.GONE);
                if (throwable instanceof IOException) {
                    loadChaptersFromDevice();
                } else {
                    Toast.makeText(StoryDetailActivity.this,
                            R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void loadLibraryStatus() {
        String authorization = requireAuthorization();
        if (authorization == null) return;
        libraryCall = ApiClient.api().getLibrary(authorization, 0, 50);
        libraryCall.enqueue(new Callback<PageResponse<LibraryItemResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<LibraryItemResponse>> call,
                                   Response<PageResponse<LibraryItemResponse>> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
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
                if (!isFinishing() && !isDestroyed() && !call.isCanceled()) {
                    Toast.makeText(StoryDetailActivity.this,
                            "Không kiểm tra được trạng thái tủ sách.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void changeLibraryMembership() {
        String authorization = requireAuthorization();
        if (authorization == null || storyId <= 0) return;
        binding.btnBookmark.setEnabled(false);
        libraryMutationCall = isSaved
                ? ApiClient.api().removeFromLibrary(authorization, storyId)
                : ApiClient.api().addToLibrary(authorization,
                        new com.example.android_app.model.AddLibraryRequest(storyId));
        libraryMutationCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
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
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                binding.btnBookmark.setEnabled(true);
                Toast.makeText(StoryDetailActivity.this,
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateLibraryButton() {
        binding.btnBookmark.setText(isSaved ? "Đã lưu tủ sách" : "Lưu tủ sách");
    }

    private void openReader(long chapterId) {
        Intent intent = new Intent(this, ReaderActivity.class);
        intent.putExtra("story_id", storyId);
        intent.putExtra("story_name", storyName);
        intent.putExtra("chapter_id", chapterId);
        intent.putExtra("chapter_number", chapterNumber(chapterId));
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

    private String requireAuthorization() {
        String authorization = ApiClient.authorization(this);
        if (authorization == null) {
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
