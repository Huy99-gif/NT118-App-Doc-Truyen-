package com.example.android_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android_app.databinding.ActivityLoginBinding;
import com.example.android_app.model.LoginRequest;
import com.example.android_app.model.LoginResponse;
import com.example.android_app.network.ApiClient;
import com.example.android_app.util.ApiErrorParser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private Call<LoginResponse> loginCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    @Override
    protected void onDestroy() {
        if (loginCall != null) {
            loginCall.cancel();
        }
        super.onDestroy();
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
        String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";

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

        showLoading(true);
        loginCall = ApiClient.auth().login(new LoginRequest(email, password));
        loginCall.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                showLoading(false);

                if (!response.isSuccessful()) {
                    Toast.makeText(LoginActivity.this,
                            ApiErrorParser.getMessage(response, getString(R.string.error_login_failed)),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                LoginResponse loginResponse = response.body();
                if (loginResponse == null || TextUtils.isEmpty(loginResponse.getToken())) {
                    Toast.makeText(LoginActivity.this, R.string.error_login_response, Toast.LENGTH_LONG).show();
                    return;
                }

                AuthPreferences.saveToken(LoginActivity.this, loginResponse.getToken());
                Toast.makeText(LoginActivity.this, R.string.toast_login_success, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable throwable) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(LoginActivity.this, R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onForgotPasswordClicked() {
        startActivity(new Intent(this, ForgotPasswordActivity.class));
    }

    private void onGuestLoginClicked() {
        Toast.makeText(this, getString(R.string.toast_guest_login), Toast.LENGTH_SHORT).show();
        // Chuyển thẳng sang màn hình chính
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void onRegisterClicked() {
        startActivity(new Intent(this, RegisterActivity.class));
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
