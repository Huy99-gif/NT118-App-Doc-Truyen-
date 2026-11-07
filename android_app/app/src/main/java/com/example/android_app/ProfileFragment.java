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

import com.example.android_app.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadUserData();
        setupListeners();
    }

    private void loadUserData() {
        // [GIẢ LẬP API] Sau này nối Retrofit endpoint GET /auth/me
        // ApiService.getCurrentUser().enqueue(...)

        binding.tvDisplayName.setText("Nguyễn Văn A");
        binding.tvEmail.setText("nguyenvana@gmail.com");
        binding.tvRole.setText("Độc giả VIP");

        binding.tvStatSaved.setText("12");
        binding.tvStatHistory.setText("45");
        binding.tvStatLiked.setText("8");
    }

    private void setupListeners() {
        // Chỉnh sửa thông tin
        binding.btnEditProfile.setOnClickListener(v -> {
            // [GIẢ LẬP] Mở màn hình chỉnh sửa hồ sơ
            Toast.makeText(requireContext(), "Mở màn hình Chỉnh sửa thông tin cá nhân", Toast.LENGTH_SHORT).show();
        });

        // Đổi mật khẩu
        binding.btnChangePassword.setOnClickListener(v -> {
            // [GIẢ LẬP] Mở màn hình đổi mật khẩu
            Toast.makeText(requireContext(), "Mở màn hình Đổi mật khẩu", Toast.LENGTH_SHORT).show();
        });

        // Tủ sách cá nhân
        binding.btnMyLibrary.setOnClickListener(v -> {
            // [GIẢ LẬP] Chuyển tab hoặc mở tủ sách
            Toast.makeText(requireContext(), "Mở Tủ sách cá nhân", Toast.LENGTH_SHORT).show();
        });

        // Lịch sử đọc
        binding.btnReadingHistory.setOnClickListener(v -> {
            // [GIẢ LẬP] Mở lịch sử đọc
            Toast.makeText(requireContext(), "Mở Lịch sử đọc truyện", Toast.LENGTH_SHORT).show();
        });

        // Đăng xuất
        binding.btnLogout.setOnClickListener(v -> handleLogout());
    }

    private void handleLogout() {
        // [GIẢ LẬP API / LOGIC] Xóa token khỏi TokenManager/SharedPreferences
        // TokenManager.getInstance(requireContext()).clearToken();

        Toast.makeText(requireContext(), "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
