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
import com.example.android_app.model.UserResponse;
import com.example.android_app.network.ApiClient;
import com.example.android_app.util.ApiErrorParser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private Call<UserResponse> currentUserCall;

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
        String token = AuthPreferences.getToken(requireContext());
        if (token == null || token.trim().isEmpty()) {
            binding.tvDisplayName.setText(R.string.profile_guest_name);
            binding.tvEmail.setText("");
            binding.tvRole.setText("");
            clearProfileStats();
            return;
        }

        currentUserCall = ApiClient.auth().getCurrentUser("Bearer " + token);
        currentUserCall.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (binding == null) return;
                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(),
                            ApiErrorParser.getMessage(response, getString(R.string.error_profile_load_failed)),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                UserResponse user = response.body();
                if (user == null) {
                    Toast.makeText(requireContext(),
                            R.string.error_profile_load_failed, Toast.LENGTH_LONG).show();
                    return;
                }

                binding.tvDisplayName.setText(user.getDisplayName());
                binding.tvEmail.setText(user.getEmail());
                binding.tvRole.setText(R.string.profile_reader_role);
                clearProfileStats();
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable throwable) {
                if (binding == null || call.isCanceled()) return;
                Toast.makeText(requireContext(),
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void clearProfileStats() {
        binding.tvStatSaved.setText("—");
        binding.tvStatHistory.setText("—");
        binding.tvStatLiked.setText("—");
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
        AuthPreferences.clear(requireContext());
        Toast.makeText(requireContext(), "Đã đăng xuất tài khoản", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        if (currentUserCall != null) {
            currentUserCall.cancel();
        }
        super.onDestroyView();
        binding = null;
    }
}
