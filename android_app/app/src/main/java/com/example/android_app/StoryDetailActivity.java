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

import java.util.ArrayList;
import java.util.List;

public class StoryDetailActivity extends AppCompatActivity {

    private ActivityStoryDetailBinding binding;
    private ChapterAdapter chapterAdapter;
    private boolean isBookmarked = false;

    // Các biến nhận từ Intent
    private String storyId;
    private String storyName;
    private String storyAuthor;
    private String storyCover;
    private int chapterCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStoryDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getIntentData();
        setupUI();
        setupListeners();
        loadStoryDetail();
    }

    private void getIntentData() {
        if (getIntent() != null) {
            storyId = getIntent().getStringExtra("story_id");
            storyName = getIntent().getStringExtra("story_name");
            storyAuthor = getIntent().getStringExtra("story_author");
            storyCover = getIntent().getStringExtra("story_cover");
            chapterCount = getIntent().getIntExtra("story_chapter_count", 0);
        }
    }

    private void setupUI() {
        binding.tvStoryName.setText(storyName != null ? storyName : "Tên truyện chưa cập nhật");
        binding.tvAuthor.setText("Tác giả: " + (storyAuthor != null ? storyAuthor : "Đang cập nhật"));
        binding.tvStatus.setText("Trạng thái: Đang cập nhật");
        binding.tvCategory.setText("Thể loại: Ngôn Tình, Đô Thị, Huyền Huyễn");
        binding.tvChapterHeader.setText("Danh sách chương (" + chapterCount + " chương)");

        // Render ảnh bìa
        Glide.with(this)
                .load(storyCover)
                .apply(new RequestOptions()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground))
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.ivCover);

        // Giới thiệu nội dung giả lập
        binding.tvDescription.setText(
                "Đây là câu chuyện tình cảm đầy trắc trở nhưng cũng không kém phần ngọt ngào. " +
                "Nội dung xoay quanh những thăng trầm trong cuộc sống, tình yêu và lý tưởng của các nhân vật chính. " +
                "Mời các bạn đón đọc các chương mới nhất được cập nhật liên tục!"
        );

        // RecyclerView chương truyện
        chapterAdapter = new ChapterAdapter(chapter -> {
            Intent intent = new Intent(this, ReaderActivity.class);
            intent.putExtra("story_id", storyId);
            intent.putExtra("story_name", storyName);
            intent.putExtra("chapter_id", chapter.getId());
            intent.putExtra("chapter_number", chapter.getChapterNumber());
            intent.putExtra("chapter_title", chapter.getTitle());
            intent.putExtra("total_chapters", chapterCount > 0 ? chapterCount : 102);
            startActivity(intent);
        });
        binding.rvChapters.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChapters.setAdapter(chapterAdapter);
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Nút đọc từ đầu
        binding.btnReadFirst.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReaderActivity.class);
            intent.putExtra("story_id", storyId);
            intent.putExtra("story_name", storyName);
            intent.putExtra("chapter_id", "chap_1");
            intent.putExtra("chapter_number", 1);
            intent.putExtra("chapter_title", "Hành trình bắt đầu (Phần 1)");
            intent.putExtra("total_chapters", chapterCount > 0 ? chapterCount : 102);
            startActivity(intent);
        });

        // Nút lưu tủ sách
        binding.btnBookmark.setOnClickListener(v -> {
            isBookmarked = !isBookmarked;
            // [GIẢ LẬP API] Sau này sẽ gọi POST /api/v1/libraries hoặc DELETE /api/v1/libraries/{id}
            if (isBookmarked) {
                binding.btnBookmark.setText("Đã lưu tủ sách");
                Toast.makeText(this, "Đã thêm vào tủ sách cá nhân", Toast.LENGTH_SHORT).show();
            } else {
                binding.btnBookmark.setText("Lưu tủ sách");
                Toast.makeText(this, "Đã bỏ khỏi tủ sách", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadStoryDetail() {
        // [GIẢ LẬP API] Sau này nối Retrofit:
        // ApiService.getStoryDetail(storyId).enqueue(...)
        // ApiService.getStoryChapters(storyId).enqueue(...)

        binding.progressBarChapters.setVisibility(View.VISIBLE);

        // Giả lập delay mạng 800ms để tải danh sách chương
        binding.getRoot().postDelayed(() -> {
            binding.progressBarChapters.setVisibility(View.GONE);
            List<Chapter> mockChapters = new ArrayList<>();
            int count = chapterCount > 0 ? Math.min(chapterCount, 15) : 10;
            for (int i = 1; i <= count; i++) {
                mockChapters.add(new Chapter(
                        "chap_" + i,
                        i,
                        "Hành trình bắt đầu (Phần " + i + ")",
                        "1" + (i % 9) + "/10/2024",
                        1200 + (i * 35)
                ));
            }
            chapterAdapter.setChapters(mockChapters);
        }, 800);
    }
}
