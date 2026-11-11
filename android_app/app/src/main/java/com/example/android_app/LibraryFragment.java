package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.android_app.adapter.BookmarkAdapter;
import com.example.android_app.adapter.StoryAdapter;
import com.example.android_app.databinding.FragmentLibraryBinding;
import com.example.android_app.model.BookmarkResponse;
import com.example.android_app.model.LibraryItemResponse;
import com.example.android_app.model.PageResponse;
import com.example.android_app.model.ReadingHistoryResponse;
import com.example.android_app.model.Story;
import com.example.android_app.network.ApiClient;
import com.example.android_app.util.ApiErrorParser;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryFragment extends Fragment {
    private FragmentLibraryBinding binding;
    private StoryAdapter storyAdapter;
    private BookmarkAdapter bookmarkAdapter;
    private Call<?> activeCall;
    private Call<Void> deleteCall;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupTabs();
        setupAdapters();
        loadSavedStories();
    }

    @Override
    public void onDestroyView() {
        if (activeCall != null) activeCall.cancel();
        if (deleteCall != null) deleteCall.cancel();
        super.onDestroyView();
        binding = null;
    }

    private void setupTabs() {
        binding.tabLayoutLibrary.addTab(binding.tabLayoutLibrary.newTab().setText("Tủ sách"));
        binding.tabLayoutLibrary.addTab(binding.tabLayoutLibrary.newTab().setText("Lịch sử đọc"));
        binding.tabLayoutLibrary.addTab(binding.tabLayoutLibrary.newTab().setText("Dấu trang"));
        binding.tabLayoutLibrary.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) loadSavedStories();
                else if (tab.getPosition() == 1) loadReadingHistory();
                else loadBookmarks();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    public void selectTab(int position) {
        if (binding != null && position >= 0
                && position < binding.tabLayoutLibrary.getTabCount()) {
            TabLayout.Tab tab = binding.tabLayoutLibrary.getTabAt(position);
            if (tab != null) tab.select();
        }
    }

    private void setupAdapters() {
        storyAdapter = new StoryAdapter(story -> {
            if (story.isReading() && story.getInitialChapterId() > 0) {
                Intent intent = new Intent(requireContext(), ReaderActivity.class);
                intent.putExtra("story_id", Long.parseLong(story.getId()));
                intent.putExtra("story_name", story.getName());
                intent.putExtra("chapter_id", story.getInitialChapterId());
                intent.putExtra("chapter_number", story.getChapterCount());
                intent.putExtra("char_offset", story.getCharOffset());
                startActivity(intent);
            } else {
                Intent intent = new Intent(requireContext(), StoryDetailActivity.class);
                intent.putExtra("story_id", story.getId());
                intent.putExtra("story_name", story.getName());
                intent.putExtra("story_author", story.getAuthor());
                intent.putExtra("story_cover", story.getCoverUrl());
                startActivity(intent);
            }
        });
        bookmarkAdapter = new BookmarkAdapter(new BookmarkAdapter.Listener() {
            @Override
            public void onBookmarkClick(BookmarkResponse bookmark) {
                if (bookmark.storyId == null || bookmark.chapterId == null) {
                    Toast.makeText(requireContext(), "Dấu trang thiếu thông tin chương.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(requireContext(), ReaderActivity.class);
                intent.putExtra("story_id", bookmark.storyId);
                intent.putExtra("story_name", bookmark.storyTitle);
                intent.putExtra("chapter_id", bookmark.chapterId);
                intent.putExtra("chapter_number", bookmark.chapterNo == null ? 1 : bookmark.chapterNo);
                intent.putExtra("chapter_title", bookmark.chapterTitle);
                intent.putExtra("char_offset", bookmark.charOffset == null ? 0 : bookmark.charOffset);
                startActivity(intent);
            }

            @Override
            public void onDeleteBookmark(BookmarkResponse bookmark) {
                confirmDeleteBookmark(bookmark);
            }
        });
        binding.rvLibrary.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvLibrary.setAdapter(storyAdapter);
    }

    private void loadSavedStories() {
        loadStories(false);
    }

    private void loadReadingHistory() {
        loadStories(true);
    }

    private void loadStories(boolean history) {
        String authorization = ApiClient.authorization(requireContext());
        if (authorization == null) {
            showEmpty("Vui lòng đăng nhập để xem thư viện.");
            return;
        }
        if (activeCall != null) activeCall.cancel();
        showLoading();
        if (history) {
            Call<PageResponse<ReadingHistoryResponse>> call =
                    ApiClient.api().getReadingHistory(authorization, 0, 50);
            activeCall = call;
            call.enqueue(new Callback<PageResponse<ReadingHistoryResponse>>() {
                @Override
                public void onResponse(Call<PageResponse<ReadingHistoryResponse>> call,
                                       Response<PageResponse<ReadingHistoryResponse>> response) {
                    if (!isViewActive() || call.isCanceled()) return;
                    if (!response.isSuccessful() || response.body() == null) {
                        showError(response, "Không tải được lịch sử đọc.");
                        return;
                    }
                    List<Story> stories = new ArrayList<>();
                    for (ReadingHistoryResponse item : response.body().items) {
                        Story story = new Story(String.valueOf(item.storyId), item.storyTitle,
                                "", item.coverUrl,
                                "Chương " + item.chapterNo + ": " + item.chapterTitle, 0);
                        story.setReading(true);
                        story.setInitialChapterId(item.chapterId == null ? 0 : item.chapterId);
                        story.setCharOffset(item.charOffset == null ? 0 : item.charOffset);
                        stories.add(story);
                    }
                    binding.rvLibrary.setAdapter(storyAdapter);
                    storyAdapter.setStories(stories);
                    showResults(stories.isEmpty(), "Chưa có lịch sử đọc truyện.");
                }

                @Override
                public void onFailure(Call<PageResponse<ReadingHistoryResponse>> call, Throwable throwable) {
                    if (isViewActive() && !call.isCanceled()) {
                        showFailure("Không tải được lịch sử đọc.");
                    }
                }
            });
        } else {
            Call<PageResponse<LibraryItemResponse>> call =
                    ApiClient.api().getLibrary(authorization, 0, 50);
            activeCall = call;
            call.enqueue(new Callback<PageResponse<LibraryItemResponse>>() {
                @Override
                public void onResponse(Call<PageResponse<LibraryItemResponse>> call,
                                       Response<PageResponse<LibraryItemResponse>> response) {
                    if (!isViewActive() || call.isCanceled()) return;
                    if (!response.isSuccessful() || response.body() == null) {
                        showError(response, "Không tải được tủ sách.");
                        return;
                    }
                    List<Story> stories = new ArrayList<>();
                    for (LibraryItemResponse item : response.body().items) {
                        Story story = new Story(String.valueOf(item.storyId), item.title, item.authorName,
                                item.coverUrl, null, 0);
                        story.setStatus(item.status);
                        stories.add(story);
                    }
                    binding.rvLibrary.setAdapter(storyAdapter);
                    storyAdapter.setStories(stories);
                    showResults(stories.isEmpty(), "Chưa có truyện nào trong tủ sách.");
                }

                @Override
                public void onFailure(Call<PageResponse<LibraryItemResponse>> call, Throwable throwable) {
                    if (isViewActive() && !call.isCanceled()) {
                        showFailure("Không tải được tủ sách.");
                    }
                }
            });
        }
    }

    private void loadBookmarks() {
        String authorization = ApiClient.authorization(requireContext());
        if (authorization == null) {
            showEmpty("Vui lòng đăng nhập để xem dấu trang.");
            return;
        }
        if (activeCall != null) activeCall.cancel();
        showLoading();
        Call<PageResponse<BookmarkResponse>> call =
                ApiClient.api().getBookmarks(authorization, 0, 50);
        activeCall = call;
        call.enqueue(new Callback<PageResponse<BookmarkResponse>>() {
            @Override
            public void onResponse(Call<PageResponse<BookmarkResponse>> call,
                                   Response<PageResponse<BookmarkResponse>> response) {
                if (!isViewActive() || call.isCanceled()) return;
                if (!response.isSuccessful() || response.body() == null) {
                    showError(response, "Không tải được dấu trang.");
                    return;
                }
                binding.rvLibrary.setAdapter(bookmarkAdapter);
                bookmarkAdapter.setBookmarks(response.body().items);
                showResults(response.body().items.isEmpty(), "Chưa có dấu trang nào.");
            }

            @Override
            public void onFailure(Call<PageResponse<BookmarkResponse>> call, Throwable throwable) {
                if (isViewActive() && !call.isCanceled()) showFailure("Không tải được dấu trang.");
            }
        });
    }

    private void confirmDeleteBookmark(BookmarkResponse bookmark) {
        if (bookmark.id == null) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa dấu trang")
                .setMessage("Bạn có chắc muốn xóa dấu trang này?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> deleteBookmark(bookmark.id))
                .show();
    }

    private void deleteBookmark(long bookmarkId) {
        String authorization = ApiClient.authorization(requireContext());
        if (authorization == null) {
            showEmpty("Vui lòng đăng nhập để xóa dấu trang.");
            return;
        }
        deleteCall = ApiClient.api().deleteBookmark(authorization, bookmarkId);
        deleteCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isViewActive() || call.isCanceled()) return;
                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(),
                            ApiErrorParser.getMessage(response, "Không xóa được dấu trang."),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                loadBookmarks();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                if (isViewActive() && !call.isCanceled()) {
                    showFailure("Không xóa được dấu trang.");
                }
            }
        });
    }

    private void showLoading() {
        binding.progressBarLibrary.setVisibility(View.VISIBLE);
        binding.tvEmptyLibrary.setVisibility(View.GONE);
    }

    private void showResults(boolean empty, String message) {
        binding.progressBarLibrary.setVisibility(View.GONE);
        binding.tvEmptyLibrary.setText(message);
        binding.tvEmptyLibrary.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showEmpty(String message) {
        if (binding == null) return;
        binding.progressBarLibrary.setVisibility(View.GONE);
        binding.tvEmptyLibrary.setText(message);
        binding.tvEmptyLibrary.setVisibility(View.VISIBLE);
    }

    private void showError(Response<?> response, String fallback) {
        showEmpty(ApiErrorParser.getMessage(response, fallback));
    }

    private void showFailure(String message) {
        showEmpty(message);
    }

    private boolean isViewActive() {
        return binding != null && isAdded();
    }
}
