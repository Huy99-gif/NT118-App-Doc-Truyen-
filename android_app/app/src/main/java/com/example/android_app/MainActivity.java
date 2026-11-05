package com.example.android_app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
        binding.toolbar.setNavigationIcon(R.drawable.ic_search);
    }

    private void setupRecyclerView() {
        adapter = new StoryAdapter(story -> {
            // TODO: mở màn hình chi tiết truyện
            android.widget.Toast.makeText(this, "Chi tiết: " + story.getName(),
                    android.widget.Toast.LENGTH_SHORT).show();
        });
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                // Trang chủ
            } else if (id == R.id.nav_categories) {
                // Danh mục
            } else if (id == R.id.nav_library) {
                // Thư viện
            } else if (id == R.id.nav_account) {
                // Tài khoản
            }
            return true;
        });
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
    }

    private void loadDemoData() {
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
