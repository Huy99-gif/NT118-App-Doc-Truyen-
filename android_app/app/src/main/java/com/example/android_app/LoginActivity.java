package com.example.android_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android_app.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    private void setupListeners() {
        // Nút Đăng Nhập
        binding.btnLogin.setOnClickListener(v -> handleLogin());

        // Quên Mật Khẩu
        binding.tvForgotPassword.setOnClickListener(v -> onForgotPasswordClicked());

        // Đăng Nhập Với Tư Cách Khách
        binding.btnGuestLogin.setOnClickListener(v -> onGuestLoginClicked());

        // Đăng Ký Tài Khoản
        binding.tvRegister.setOnClickListener(v -> onRegisterClicked());
    }

    private void handleLogin() {
        String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString().trim() : "";

        // Reset lỗi hiển thị trước đó
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        boolean isValid = true;

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_empty_email));
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_empty_password));
            isValid = false;
        }

        if (!isValid) return;

        // Hiển thị trạng thái đang nạp dữ liệu
        showLoading(true);

        // Giả lập kết nối API Đăng nhập backend
        binding.getRoot().postDelayed(() -> {
            showLoading(false);
            Toast.makeText(this, getString(R.string.toast_login_success), Toast.LENGTH_SHORT).show();
            // Chuyển sang MainActivity...
        }, 1500);
    }

    private void onForgotPasswordClicked() {
        Toast.makeText(this, getString(R.string.toast_forgot_password), Toast.LENGTH_SHORT).show();
    }

    private void onGuestLoginClicked() {
        Toast.makeText(this, getString(R.string.toast_guest_login), Toast.LENGTH_SHORT).show();
    }

    private void onRegisterClicked() {
        Toast.makeText(this, getString(R.string.toast_register), Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.btnLogin.setText("");
            binding.btnLogin.setEnabled(false);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.btnLogin.setText(getString(R.string.btn_login));
            binding.btnLogin.setEnabled(true);
            binding.progressBar.setVisibility(View.GONE);
        }
    }
}
