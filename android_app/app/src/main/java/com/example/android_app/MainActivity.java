package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.android_app.adapter.StoryAdapter;
import com.example.android_app.databinding.ActivityMainBinding;
import com.example.android_app.model.Story;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private StoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupRecyclerView();
        setupBottomNavigation();
        loadDemoData();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextColor(getColor(R.color.text_primary));
    }

    private void setupRecyclerView() {
        adapter = new StoryAdapter(story -> {
            // Mở màn hình chi tiết truyện với dữ liệu được truyền qua Intent
            Intent intent = new Intent(MainActivity.this, StoryDetailActivity.class);
            intent.putExtra("story_id", story.getId());
            intent.putExtra("story_name", story.getName());
            intent.putExtra("story_author", story.getAuthor());
            intent.putExtra("story_cover", story.getCoverUrl());
            intent.putExtra("story_chapter_count", story.getChapterCount());
            startActivity(intent);
        });
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                showHomeContent();
                return true;
            } else if (id == R.id.nav_library) {
                showFragment(new LibraryFragment());
                return true;
            } else if (id == R.id.nav_account) {
                showFragment(new ProfileFragment());
                return true;
            } else if (id == R.id.nav_categories) {
                // [GIẢ LẬP] Màn hình Thể loại
                Toast.makeText(this, "Màn hình Thể loại (Đang phát triển)", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    private void showHomeContent() {
        binding.layoutHomeContent.setVisibility(View.VISIBLE);
        binding.fragmentContainer.setVisibility(View.GONE);
    }

    private void showFragment(Fragment fragment) {
        binding.layoutHomeContent.setVisibility(View.GONE);
        binding.fragmentContainer.setVisibility(View.VISIBLE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    private void loadDemoData() {
        // [GIẢ LẬP API] Sau này nối GET /stories từ Backend
        List<Story> stories = new ArrayList<>();
        stories.add(new Story("1", "Cố Ấy Tên Ai", "Nhã Ngà",
                "https://i.pinimg.com/564x19/7b/19/95/19799591b014a7e0e677579f5653f82a.jpg",
                "Chương 102: Sau tất cả", 102));
        stories.add(new Story("2", "Thám Tử Lừng Danh Conan", "Aoyama Gosho",
                "https://i.pinimg.com/564x432/4b/e3/cf/4be3cf5e5a6b69a40b57a9a6d5f5d0f8.jpg",
                "Chương 1105: Dấu vết kỳ lạ", 1105));
        stories.add(new Story("3", "Đấu Phá Thương Khung", "Đường Tam",
                "https://i.pinimg.com/564x8ad/3e/3e/3e/3e3e3e3e3e3e3e3e3e3e3e3e3e3e3e.jpg",
                "Chương 432: Thăng cấp", 1020));
        stories.add(new Story("4", "Một Mình Đua Xe", "Nhã Ngà",
                "https://i.pinimg.com/564x19/7b/19/95/19799591b014a7e0e677579f5653f82a.jpg",
                "Chương 88: Kết thúc", 88));
        stories.add(new Story("5", "Bức Tường Màu Hồng", "Đặng Hải Nam",
                "https://i.pinimg.com/564x19/7b/19/95/19799591b014a7e0e677579f5653f82a.jpg",
                "Chương 12: Nắng chiều", 12));
        adapter.setStories(stories);

        binding.progressBar.setVisibility(View.GONE);
        if (stories.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
        }
    }
}
