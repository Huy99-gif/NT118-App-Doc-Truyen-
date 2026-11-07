package com.example.android_app;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android_app.databinding.ActivityReaderBinding;

public class ReaderActivity extends AppCompatActivity {

    private ActivityReaderBinding binding;

    private String storyId;
    private String storyName;
    private String chapterId;
    private int currentChapterNumber = 1;
    private int totalChapters = 102;
    private String chapterTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReaderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getIntentData();
        setupListeners();
        loadChapterContent(currentChapterNumber);
    }

    private void getIntentData() {
        if (getIntent() != null) {
            storyId = getIntent().getStringExtra("story_id");
            storyName = getIntent().getStringExtra("story_name");
            chapterId = getIntent().getStringExtra("chapter_id");
            currentChapterNumber = getIntent().getIntExtra("chapter_number", 1);
            totalChapters = getIntent().getIntExtra("total_chapters", 102);
            chapterTitle = getIntent().getStringExtra("chapter_title");
        }
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Chương trước
        binding.btnPrevChapter.setOnClickListener(v -> {
            if (currentChapterNumber > 1) {
                currentChapterNumber--;
                loadChapterContent(currentChapterNumber);
            }
        });

        // Chương sau
        binding.btnNextChapter.setOnClickListener(v -> {
            if (currentChapterNumber < totalChapters) {
                currentChapterNumber++;
                loadChapterContent(currentChapterNumber);
            } else {
                Toast.makeText(this, "Bạn đang ở chương mới nhất!", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút danh sách chương
        binding.btnChapterList.setOnClickListener(v -> {
            Toast.makeText(this, "Danh sách " + totalChapters + " chương", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadChapterContent(int chapterNum) {
        // [GIẢ LẬP API] Sau này nối Retrofit GET /api/v1/chapters/{id}
        // ApiService.getChapterContent(chapterId).enqueue(...)

        binding.progressBarReader.setVisibility(View.VISIBLE);
        binding.tvChapterContent.setVisibility(View.GONE);
        binding.scrollView.scrollTo(0, 0);

        String displayStoryName = (storyName != null ? storyName : "Chi tiết truyện");
        binding.tvToolbarTitle.setText(displayStoryName + " - Chương " + chapterNum);
        binding.tvChapterTitle.setText("Chương " + chapterNum + ": " + (chapterTitle != null && !chapterTitle.isEmpty() ? chapterTitle : "Hành trình định mệnh"));
        binding.tvChapterInfo.setText("Cập nhật: 1" + (chapterNum % 9) + "/10/2024  •  " + (1200 + chapterNum * 45) + " lượt đọc");
        binding.btnChapterList.setText("Chương " + chapterNum + " / " + totalChapters);

        // Trạng thái nút chương trước / chương sau
        binding.btnPrevChapter.setEnabled(chapterNum > 1);
        binding.btnNextChapter.setEnabled(chapterNum < totalChapters);

        // Giả lập delay tải nội dung chương (500ms)
        binding.getRoot().postDelayed(() -> {
            binding.progressBarReader.setVisibility(View.GONE);
            binding.tvChapterContent.setVisibility(View.VISIBLE);

            String mockContent = generateMockChapterContent(chapterNum);
            binding.tvChapterContent.setText(mockContent);
        }, 500);
    }

    private String generateMockChapterContent(int chapterNum) {
        return "Gió mùa thu khẽ thổi qua những tán lá vàng rực rỡ, tạo nên những âm thanh rì rào êm ái giữa không gian yên bình của buổi chiều tà.\n\n" +
               "Nhân vật chính đứng bên khung cửa sổ mở rộng, ánh mắt trầm ngâm hướng về phía chân trời xa xăm. Những ký ức của quá khứ cuồn cuộn trở về như một cuốn phim quay chậm, gợi lên bao nhiêu cảm xúc vui buồn lẫn lộn.\n\n" +
               "\"Cuối cùng thì thời khắc này cũng đã tới,\" anh khẽ thì thầm với chính mình, bàn tay siết chặt chiếc nhẫn bằng ngọc thạch – vật gia bảo duy nhất mà người thân để lại.\n\n" +
               "Ở ngoài kia, cuộc phiêu lưu thực sự của Chương " + chapterNum + " chỉ mới vừa bắt đầu. Những thử thách cam go, những đối thủ hùng mạnh và cả những bí mật kinh thiên động địa đang chờ đợi phía trước...\n\n" +
               "Bất ngờ từ xa có tiếng bước chân dồn dập tiến lại gần. Một bóng người xuất hiện trong làn sương mờ, cất tiếng gọi thất thanh:\n\n" +
               "\"Mọi chuyện không xong rồi! Chúng ta phải khởi hành ngay lập tức!\"\n\n" +
               "(Hết Chương " + chapterNum + " - Mời bạn bấm \"Chương sau\" để tiếp tục đọc)";
    }
}
