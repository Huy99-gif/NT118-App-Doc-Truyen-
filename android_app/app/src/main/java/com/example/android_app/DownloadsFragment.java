package com.example.android_app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.android_app.adapter.DownloadedStoryAdapter;
import com.example.android_app.databinding.FragmentDownloadsBinding;
import com.example.android_app.offline.OfflineExecutors;
import com.example.android_app.offline.StorageFormat;
import com.example.android_app.offline.db.ChapterHeader;
import com.example.android_app.offline.db.DownloadedStorySummary;
import com.example.android_app.offline.db.LocalProgressEntity;
import com.example.android_app.offline.db.OfflineDao;
import com.example.android_app.offline.db.OfflineDatabase;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Truyện và chương đã tải, đọc hoàn toàn từ Room (không gọi mạng).
 * Dùng trong tab "Đã tải" của Tủ sách và trong OfflineLibraryActivity (khi không còn phiên).
 */
public class DownloadsFragment extends Fragment {
    private static final String ARG_OFFLINE_MODE = "offline_mode";

    private FragmentDownloadsBinding binding;
    private DownloadedStoryAdapter adapter;
    private String ownerKey;
    private boolean offlineMode;
    private DownloadedStorySummary openedStory;
    private List<ChapterHeader> openedChapters = new ArrayList<>();
    private LiveData<List<ChapterHeader>> chapterSource;
    private final OnBackPressedCallback backToStories = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            showStories();
        }
    };

    public static DownloadsFragment newInstance(boolean offlineMode) {
        DownloadsFragment fragment = new DownloadsFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_OFFLINE_MODE, offlineMode);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDownloadsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        offlineMode = getArguments() != null && getArguments().getBoolean(ARG_OFFLINE_MODE, false);
        ownerKey = AuthPreferences.getOwnerKey(requireContext());
        adapter = new DownloadedStoryAdapter(new DownloadedStoryAdapter.Listener() {
            @Override
            public void onRowClick(DownloadedStoryAdapter.Row row) {
                if (row.payload instanceof DownloadedStorySummary) {
                    openStory((DownloadedStorySummary) row.payload);
                } else if (row.payload instanceof ChapterHeader) {
                    openChapter((ChapterHeader) row.payload);
                }
            }

            @Override
            public void onRowLongClick(DownloadedStoryAdapter.Row row) {
                confirmDelete(row);
            }
        });
        binding.rvDownloads.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvDownloads.setAdapter(adapter);
        binding.btnBackToStories.setOnClickListener(v -> showStories());
        requireActivity().getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), backToStories);

        if (ownerKey == null) {
            showEmpty("Chưa có truyện nào được tải.");
            return;
        }
        dao().observeStories(ownerKey).observe(getViewLifecycleOwner(), this::renderStories);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private OfflineDao dao() {
        return OfflineDatabase.get(requireContext()).dao();
    }

    private List<DownloadedStorySummary> lastStories = new ArrayList<>();

    private void renderStories(List<DownloadedStorySummary> stories) {
        lastStories = stories;
        if (openedStory != null || binding == null) return;
        List<DownloadedStoryAdapter.Row> rows = new ArrayList<>();
        for (DownloadedStorySummary story : stories) {
            String subtitle = story.chapterCount + " chương · " + StorageFormat.formatBytes(story.totalBytes);
            if (story.totalChapters > 0 && story.chapterCount >= story.totalChapters) {
                subtitle += " · Cả truyện";
            }
            rows.add(new DownloadedStoryAdapter.Row(valueOrDefault(story.title, "Truyện đã tải"),
                    subtitle, story.coverUrl, true, story));
        }
        adapter.setRows(rows);
        if (rows.isEmpty()) showEmpty("Chưa có truyện nào được tải.");
        else binding.tvEmptyDownloads.setVisibility(View.GONE);
    }

    private void openStory(DownloadedStorySummary story) {
        openedStory = story;
        backToStories.setEnabled(true);
        binding.layoutStoryHeader.setVisibility(View.VISIBLE);
        binding.tvDownloadsHeader.setText(valueOrDefault(story.title, "Truyện đã tải"));
        if (chapterSource != null) chapterSource.removeObservers(getViewLifecycleOwner());
        chapterSource = dao().observeChapterHeaders(ownerKey, story.storyId);
        chapterSource.observe(getViewLifecycleOwner(), this::renderChapters);
    }

    private void renderChapters(List<ChapterHeader> chapters) {
        if (openedStory == null || binding == null) return;
        // xoá hết chương thì truyện cũng biến mất: quay về danh sách truyện
        if (chapters.isEmpty()) {
            showStories();
            return;
        }
        openedChapters = chapters;
        List<DownloadedStoryAdapter.Row> rows = new ArrayList<>();
        for (ChapterHeader chapter : chapters) {
            rows.add(new DownloadedStoryAdapter.Row(
                    "Chương " + chapter.chapterNo + ": " + valueOrDefault(chapter.title, ""),
                    StorageFormat.formatBytes(chapter.sizeBytes), null, false, chapter));
        }
        adapter.setRows(rows);
        binding.tvEmptyDownloads.setVisibility(View.GONE);
    }

    private void showStories() {
        openedStory = null;
        backToStories.setEnabled(false);
        if (chapterSource != null) {
            chapterSource.removeObservers(getViewLifecycleOwner());
            chapterSource = null;
        }
        if (binding == null) return;
        binding.layoutStoryHeader.setVisibility(View.GONE);
        renderStories(lastStories);
    }

    private void openChapter(ChapterHeader chapter) {
        DownloadedStorySummary story = openedStory;
        if (story == null) return;
        List<ChapterHeader> chapters = new ArrayList<>(openedChapters);
        String owner = ownerKey;
        Context context = requireContext().getApplicationContext();
        OfflineExecutors.io().execute(() -> {
            LocalProgressEntity progress = OfflineDatabase.get(context).dao()
                    .findLocalProgress(owner, story.storyId);
            int offset = progress != null && progress.chapterId == chapter.chapterId
                    ? progress.charOffset : 0;
            OfflineExecutors.main(() -> {
                if (!isAdded()) return;
                startActivity(readerIntent(story, chapters, chapter, offset));
            });
        });
    }

    private Intent readerIntent(DownloadedStorySummary story, List<ChapterHeader> chapters,
                                ChapterHeader chapter, int offset) {
        long[] ids = new long[chapters.size()];
        int[] numbers = new int[chapters.size()];
        String[] titles = new String[chapters.size()];
        for (int i = 0; i < chapters.size(); i++) {
            ids[i] = chapters.get(i).chapterId;
            numbers[i] = chapters.get(i).chapterNo;
            titles[i] = chapters.get(i).title;
        }
        return new Intent(requireContext(), ReaderActivity.class)
                .putExtra("story_id", story.storyId)
                .putExtra("story_name", story.title)
                .putExtra("chapter_id", chapter.chapterId)
                .putExtra("chapter_number", chapter.chapterNo)
                .putExtra("chapter_title", chapter.title)
                .putExtra("total_chapters", story.totalChapters)
                .putExtra("char_offset", offset)
                .putExtra("chapter_ids", ids)
                .putExtra("chapter_numbers", numbers)
                .putExtra("chapter_titles", titles)
                .putExtra(ReaderActivity.EXTRA_OFFLINE_MODE, offlineMode);
    }

    private void confirmDelete(DownloadedStoryAdapter.Row row) {
        String owner = ownerKey;
        Context context = requireContext().getApplicationContext();
        if (row.payload instanceof DownloadedStorySummary) {
            DownloadedStorySummary story = (DownloadedStorySummary) row.payload;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Xoá truyện đã tải?")
                    .setMessage("Toàn bộ " + story.chapterCount + " chương của \""
                            + valueOrDefault(story.title, "truyện") + "\" sẽ bị xoá khỏi máy.")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Xoá", (dialog, which) -> OfflineExecutors.io().execute(() ->
                            OfflineDatabase.get(context).dao().deleteStory(owner, story.storyId)))
                    .show();
        } else if (row.payload instanceof ChapterHeader) {
            ChapterHeader chapter = (ChapterHeader) row.payload;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Xoá chương đã tải?")
                    .setMessage("Chương " + chapter.chapterNo + " sẽ bị xoá khỏi máy.")
                    .setNegativeButton("Hủy", null)
                    .setPositiveButton("Xoá", (dialog, which) -> OfflineExecutors.io().execute(() ->
                            OfflineDatabase.get(context).dao()
                                    .deleteChapter(owner, chapter.storyId, chapter.chapterId)))
                    .show();
        }
    }

    private void showEmpty(String message) {
        if (binding == null) return;
        adapter.setRows(null);
        binding.tvEmptyDownloads.setText(message);
        binding.tvEmptyDownloads.setVisibility(View.VISIBLE);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
