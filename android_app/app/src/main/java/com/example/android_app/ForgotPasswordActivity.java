package com.example.android_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.android_app.databinding.ActivityForgotPasswordBinding;
import com.example.android_app.model.ForgotPasswordRequest;
import com.example.android_app.model.MessageResponse;
import com.example.android_app.model.ResetPasswordRequest;
import com.example.android_app.network.ApiClient;
import com.example.android_app.util.ApiErrorParser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final int MAX_TOKEN_LENGTH = 255;

    private ActivityForgotPasswordBinding binding;
    private String verifiedEmail = "";
    private Call<MessageResponse> forgotPasswordCall;
    private Call<MessageResponse> resetPasswordCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
        setupBackNavigation();
        showEmailStep();
    }

    @Override
    protected void onDestroy() {
        if (forgotPasswordCall != null) {
            forgotPasswordCall.cancel();
        }
        if (resetPasswordCall != null) {
            resetPasswordCall.cancel();
        }
        super.onDestroy();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.tvBackToLogin.setOnClickListener(v -> finish());
        binding.btnSendCode.setOnClickListener(v -> handleSendCode());
        binding.btnResetPassword.setOnClickListener(v -> handleResetPassword());
        binding.tvResendCode.setOnClickListener(v -> handleResendCode());
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.layoutStepReset.getVisibility() == View.VISIBLE) {
                    showEmailStep();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void handleSendCode() {
        String email = getText(binding.etEmail);
        binding.tilEmail.setError(null);

        if (!isValidEmail(email)) {
            return;
        }

        showSendCodeLoading(true);
        requestPasswordReset(email, false);
    }

    private void handleResendCode() {
        if (!isValidEmail(verifiedEmail)) {
            showEmailStep();
            return;
        }

        binding.tvResendCode.setEnabled(false);
        requestPasswordReset(verifiedEmail, true);
    }

    private void handleResetPassword() {
        String token = getText(binding.etToken);
        String newPassword = getRawText(binding.etNewPassword);
        String confirmPassword = getRawText(binding.etConfirmPassword);

        binding.tilToken.setError(null);
        binding.tilNewPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        boolean isValid = true;

        if (TextUtils.isEmpty(token)) {
            binding.tilToken.setError(getString(R.string.error_empty_token));
            isValid = false;
        } else if (token.length() > MAX_TOKEN_LENGTH) {
            binding.tilToken.setError(getString(R.string.error_invalid_token));
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

        if (!isValid) {
            return;
        }

        showResetLoading(true);
        resetPasswordCall = ApiClient.auth().resetPassword(
                new ResetPasswordRequest(token, newPassword));
        resetPasswordCall.enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                showResetLoading(false);

                if (!response.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this,
                            ApiErrorParser.getMessage(response, getString(R.string.error_reset_password_failed)),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                String message = response.body() != null
                        ? response.body().getMessage() : getString(R.string.toast_reset_success);
                Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable throwable) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                showResetLoading(false);
                Toast.makeText(ForgotPasswordActivity.this,
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void requestPasswordReset(String email, boolean resend) {
        forgotPasswordCall = ApiClient.auth().forgotPassword(new ForgotPasswordRequest(email));
        forgotPasswordCall.enqueue(new Callback<MessageResponse>() {
            @Override
            public void onResponse(Call<MessageResponse> call, Response<MessageResponse> response) {
                if (isFinishing() || isDestroyed()) return;
                if (resend) {
                    binding.tvResendCode.setEnabled(true);
                } else {
                    showSendCodeLoading(false);
                }

                if (!response.isSuccessful()) {
                    String fallback = getString(R.string.error_forgot_password_failed);
                    Toast.makeText(ForgotPasswordActivity.this,
                            ApiErrorParser.getMessage(response, fallback), Toast.LENGTH_LONG).show();
                    return;
                }

                String message = response.body() != null
                        ? response.body().getMessage() : getString(R.string.toast_code_sent);
                if (resend) {
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                } else {
                    verifiedEmail = email;
                    showResetStep();
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MessageResponse> call, Throwable throwable) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                if (resend) {
                    binding.tvResendCode.setEnabled(true);
                } else {
                    showSendCodeLoading(false);
                }
                Toast.makeText(ForgotPasswordActivity.this,
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_empty_email_only));
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return false;
        }
        return true;
    }

    private void showEmailStep() {
        binding.tvTitle.setText(R.string.forgot_password_title);
        binding.tvSubtitle.setText(R.string.forgot_password_subtitle);
        binding.layoutStepEmail.setVisibility(View.VISIBLE);
        binding.layoutStepReset.setVisibility(View.GONE);
    }

    private void showResetStep() {
        binding.tvTitle.setText(R.string.reset_password_title);
        binding.tvSubtitle.setText(R.string.reset_password_subtitle);
        binding.tvEmailSent.setText(getString(R.string.forgot_password_email_sent, verifiedEmail));
        binding.layoutStepEmail.setVisibility(View.GONE);
        binding.layoutStepReset.setVisibility(View.VISIBLE);
        binding.etToken.setText("");
        binding.etNewPassword.setText("");
        binding.etConfirmPassword.setText("");
        binding.tilToken.setError(null);
        binding.tilNewPassword.setError(null);
        binding.tilConfirmPassword.setError(null);
    }

    private void showSendCodeLoading(boolean isLoading) {
        if (isLoading) {
            binding.btnSendCode.setText("");
            binding.btnSendCode.setEnabled(false);
            binding.progressSendCode.setVisibility(View.VISIBLE);
        } else {
            binding.btnSendCode.setText(R.string.btn_send_code);
            binding.btnSendCode.setEnabled(true);
            binding.progressSendCode.setVisibility(View.GONE);
        }
    }

    private void showResetLoading(boolean isLoading) {
        if (isLoading) {
            binding.btnResetPassword.setText("");
            binding.btnResetPassword.setEnabled(false);
            binding.progressReset.setVisibility(View.VISIBLE);
        } else {
            binding.btnResetPassword.setText(R.string.btn_reset_password);
            binding.btnResetPassword.setEnabled(true);
            binding.progressReset.setVisibility(View.GONE);
        }
    }

    private String getText(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getRawText(EditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }
}
