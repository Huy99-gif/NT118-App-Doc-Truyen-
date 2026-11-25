package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android_app.databinding.ActivityOfflineLibraryBinding;

/**
 * Truyện đã tải của tài khoản đăng nhập gần nhất, mở từ màn đăng nhập khi không còn phiên.
 * Không gọi mạng; App.java không đẩy màn này về đăng nhập.
 */
public class OfflineLibraryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityOfflineLibraryBinding binding = ActivityOfflineLibraryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.btnGoLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.offlineLibraryContainer, DownloadsFragment.newInstance(true))
                    .commit();
        }
    }
}
