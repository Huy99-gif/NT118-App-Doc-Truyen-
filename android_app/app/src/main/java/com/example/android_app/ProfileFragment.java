package com.example.android_app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadUserData();
                }
            });

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

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
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

        if (currentUserCall != null) currentUserCall.cancel();
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
            String token = AuthPreferences.getToken(requireContext());
            if (token == null || token.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để chỉnh sửa thông tin.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(requireContext(), EditProfileActivity.class);
            intent.putExtra("email", binding.tvEmail.getText().toString());
            intent.putExtra("display_name", binding.tvDisplayName.getText().toString());
            editProfileLauncher.launch(intent);
        });

        // Đổi mật khẩu
        binding.btnChangePassword.setOnClickListener(v -> {
            String token = AuthPreferences.getToken(requireContext());
            if (token == null || token.trim().isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để đổi mật khẩu.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(requireContext(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        // Tủ sách cá nhân
        binding.btnMyLibrary.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).openLibraryTab(0);
        });

        // Lịch sử đọc
        binding.btnReadingHistory.setOnClickListener(v -> {
            ((MainActivity) requireActivity()).openLibraryTab(1);
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
