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

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final int MAX_TOKEN_LENGTH = 255;

    private ActivityForgotPasswordBinding binding;
    private String verifiedEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
        setupBackNavigation();
        showEmailStep();
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
        binding.getRoot().postDelayed(() -> {
            showSendCodeLoading(false);
            verifiedEmail = email;
            showResetStep();
            Toast.makeText(this, getString(R.string.toast_code_sent), Toast.LENGTH_SHORT).show();
        }, 1500);
    }

    private void handleResendCode() {
        if (!isValidEmail(verifiedEmail)) {
            showEmailStep();
            return;
        }

        binding.tvResendCode.setEnabled(false);
        binding.getRoot().postDelayed(() -> {
            binding.tvResendCode.setEnabled(true);
            Toast.makeText(this, getString(R.string.toast_code_resent), Toast.LENGTH_SHORT).show();
        }, 1200);
    }

    private void handleResetPassword() {
        String token = getText(binding.etToken);
        String newPassword = getText(binding.etNewPassword);
        String confirmPassword = getText(binding.etConfirmPassword);

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

        if (TextUtils.isEmpty(newPassword)) {
            binding.tilNewPassword.setError(getString(R.string.error_empty_password));
            isValid = false;
        } else if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            binding.tilNewPassword.setError(getString(R.string.error_password_too_short));
            isValid = false;
        } else if (newPassword.length() > MAX_PASSWORD_LENGTH) {
            binding.tilNewPassword.setError(getString(R.string.error_password_too_long));
            isValid = false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
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
        binding.getRoot().postDelayed(() -> {
            showResetLoading(false);
            Toast.makeText(this, getString(R.string.toast_reset_success), Toast.LENGTH_SHORT).show();
            finish();
        }, 1500);
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
}
