package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.android_app.adapter.StoryAdapter;
import com.example.android_app.databinding.FragmentLibraryBinding;
import com.example.android_app.model.Story;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

    private FragmentLibraryBinding binding;
    private StoryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupTabs();
        setupRecyclerView();
        loadSavedStories();
    }

    private void setupTabs() {
        binding.tabLayoutLibrary.addTab(binding.tabLayoutLibrary.newTab().setText("Tủ sách đã lưu"));
        binding.tabLayoutLibrary.addTab(binding.tabLayoutLibrary.newTab().setText("Lịch sử đọc"));

        binding.tabLayoutLibrary.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    loadSavedStories();
                } else {
                    loadReadingHistory();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new StoryAdapter(story -> {
            Intent intent = new Intent(requireContext(), StoryDetailActivity.class);
            intent.putExtra("story_id", story.getId());
            intent.putExtra("story_name", story.getName());
            intent.putExtra("story_author", story.getAuthor());
            intent.putExtra("story_cover", story.getCoverUrl());
            intent.putExtra("story_chapter_count", story.getChapterCount());
            startActivity(intent);
        });
        binding.rvLibrary.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvLibrary.setAdapter(adapter);
    }

    private void loadSavedStories() {
        // [GIẢ LẬP API] Nối GET /api/v1/libraries từ Backend
        binding.progressBarLibrary.setVisibility(View.VISIBLE);
        binding.tvEmptyLibrary.setVisibility(View.GONE);

        binding.getRoot().postDelayed(() -> {
            if (binding == null) return;
            binding.progressBarLibrary.setVisibility(View.GONE);

            List<Story> savedStories = new ArrayList<>();
            savedStories.add(new Story("1", "Cố Ấy Tên Ai", "Nhã Ngà",
                    "https://i.pinimg.com/564x19/7b/19/95/19799591b014a7e0e677579f5653f82a.jpg",
                    "Chương 102: Sau tất cả", 102));
            savedStories.add(new Story("3", "Đấu Phá Thương Khung", "Đường Tam",
                    "https://i.pinimg.com/564x8ad/3e/3e/3e/3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e.jpg",
                    "Chương 432: Thăng cấp", 1020));

            adapter.setStories(savedStories);
            if (savedStories.isEmpty()) {
                binding.tvEmptyLibrary.setVisibility(View.VISIBLE);
                binding.tvEmptyLibrary.setText("Chưa có truyện nào trong tủ sách");
            }
        }, 500);
    }

    private void loadReadingHistory() {
        // [GIẢ LẬP API] Nối GET /api/v1/history từ Backend
        binding.progressBarLibrary.setVisibility(View.VISIBLE);
        binding.tvEmptyLibrary.setVisibility(View.GONE);

        binding.getRoot().postDelayed(() -> {
            if (binding == null) return;
            binding.progressBarLibrary.setVisibility(View.GONE);

            List<Story> historyStories = new ArrayList<>();
            Story s1 = new Story("2", "Thám Tử Lừng Danh Conan", "Aoyama Gosho",
                    "https://i.pinimg.com/564x432/4b/e3/cf/4be3cf5e5a6b69a40b57a9a6d5f5d0f8.jpg",
                    "Chương 1105: Dấu vết kỳ lạ", 1105);
            s1.setReading(true);

            Story s2 = new Story("4", "Một Mình Đua Xe", "Nhã Ngà",
                    "https://i.pinimg.com/564x19/7b/19/95/19799591b014a7e0e677579f5653f82a.jpg",
                    "Chương 88: Kết thúc", 88);
            s2.setReading(true);

            historyStories.add(s1);
            historyStories.add(s2);

            adapter.setStories(historyStories);
            if (historyStories.isEmpty()) {
                binding.tvEmptyLibrary.setVisibility(View.VISIBLE);
                binding.tvEmptyLibrary.setText("Chưa có lịch sử đọc truyện");
            }
        }, 500);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
