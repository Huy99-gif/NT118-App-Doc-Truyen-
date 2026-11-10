package com.example.android_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android_app.databinding.ActivityRegisterBinding;
import com.example.android_app.model.RegisterRequest;
import com.example.android_app.model.RegisterResponse;
import com.example.android_app.network.ApiClient;
import com.example.android_app.util.ApiErrorParser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final int MAX_DISPLAY_NAME_LENGTH = 100;

    private ActivityRegisterBinding binding;
    private Call<RegisterResponse> registerCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
    }

    @Override
    protected void onDestroy() {
        if (registerCall != null) {
            registerCall.cancel();
        }
        super.onDestroy();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.tvBackToLogin.setOnClickListener(v -> finish());
        binding.btnRegister.setOnClickListener(v -> handleRegister());
    }

    private void handleRegister() {
        String displayName = getText(binding.etDisplayName);
        String email = getText(binding.etEmail);
        String password = getRawText(binding.etPassword);
        String confirmPassword = getRawText(binding.etConfirmPassword);

        binding.tilDisplayName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        boolean isValid = true;

        if (TextUtils.isEmpty(displayName)) {
            binding.tilDisplayName.setError(getString(R.string.error_empty_display_name));
            isValid = false;
        } else if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            binding.tilDisplayName.setError(getString(R.string.error_display_name_too_long));
            isValid = false;
        }

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_empty_email_only));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            isValid = false;
        }

        if (TextUtils.isEmpty(password.trim())) {
            binding.tilPassword.setError(getString(R.string.error_empty_password));
            isValid = false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            binding.tilPassword.setError(getString(R.string.error_password_too_short));
            isValid = false;
        } else if (password.length() > MAX_PASSWORD_LENGTH) {
            binding.tilPassword.setError(getString(R.string.error_password_too_long));
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword.trim())) {
            binding.tilConfirmPassword.setError(getString(R.string.error_empty_confirm_register_password));
            isValid = false;
        } else if (!TextUtils.isEmpty(password) && !password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        showLoading(true);
        registerCall = ApiClient.auth().register(
                new RegisterRequest(email, password, displayName));
        registerCall.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                showLoading(false);

                if (!response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this,
                            ApiErrorParser.getMessage(response, getString(R.string.error_register_failed)),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(RegisterActivity.this,
                        R.string.toast_register_success, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable throwable) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(RegisterActivity.this,
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.btnRegister.setText("");
            binding.btnRegister.setEnabled(false);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.btnRegister.setText(R.string.btn_create_account);
            binding.btnRegister.setEnabled(true);
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private String getText(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getRawText(EditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
}
