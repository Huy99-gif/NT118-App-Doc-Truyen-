package com.example.android_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android_app.databinding.ActivityChangePasswordBinding;
import com.example.android_app.model.ChangePasswordRequest;
import com.example.android_app.model.MessageResponse;
import com.example.android_app.network.ApiClient;
import com.example.android_app.util.ApiErrorParser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private ActivityChangePasswordBinding binding;
    private Call<MessageResponse> changePasswordCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    @Override
    protected void onDestroy() {
        if (changePasswordCall != null) {
            changePasswordCall.cancel();
        }
        super.onDestroy();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnChangePassword.setOnClickListener(v -> handleChangePassword());
    }

    private void handleChangePassword() {
        String currentPassword = getRawText(binding.etCurrentPassword);
        String newPassword = getRawText(binding.etNewPassword);
        String confirmPassword = getRawText(binding.etConfirmPassword);

        binding.tilCurrentPassword.setError(null);
        binding.tilNewPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        boolean isValid = true;

        if (TextUtils.isEmpty(currentPassword.trim())) {
            binding.tilCurrentPassword.setError(getString(R.string.error_empty_current_password));
            isValid = false;
        }

        if (TextUtils.isEmpty(newPassword.trim())) {
            binding.tilNewPassword.setError(getString(R.string.error_empty_password));
            isValid = false;
        } else if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            binding.tilNewPassword.setError(getString(R.string.error_password_too_short));
            isValid = false;
        } else if (newPassword.length() > MAX_PASSWORD_LENGTH) {
            binding.tilNewPassword.setError(getString(R.string.error_password_too_long));
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword.trim())) {
            binding.tilConfirmPassword.setError(getString(R.string.error_empty_confirm_password));
            isValid = false;
        } else if (!TextUtils.isEmpty(newPassword) && !newPassword.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            isValid = false;
        }

        if (!isValid) return;

        String authorization = ApiClient.authorization(this);
        if (authorization == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thực hiện chức năng này.", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);
        changePasswordCall = ApiClient.auth().changePassword(
                authorization, new ChangePasswordRequest(currentPassword, newPassword));
        changePasswordCall.enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                showLoading(false);

                if (!response.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this,
                            ApiErrorParser.getMessage(response, getString(R.string.error_change_password_failed)),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String message = response.body() != null && !TextUtils.isEmpty(response.body().getMessage())
                        ? response.body().getMessage() : getString(R.string.toast_change_password_success);

                Toast.makeText(ChangePasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable throwable) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(ChangePasswordActivity.this,
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.btnChangePassword.setText("");
            binding.btnChangePassword.setEnabled(false);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.btnChangePassword.setText(R.string.change_password_title);
            binding.btnChangePassword.setEnabled(true);
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private String getRawText(EditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
}
