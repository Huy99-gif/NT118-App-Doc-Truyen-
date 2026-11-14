package com.example.android_app;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android_app.databinding.ActivityEditProfileBinding;
import com.example.android_app.model.UpdateProfileRequest;
import com.example.android_app.model.UserResponse;
import com.example.android_app.network.ApiClient;
import com.example.android_app.util.ApiErrorParser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private static final int MAX_DISPLAY_NAME_LENGTH = 100;

    private ActivityEditProfileBinding binding;
    private Call<UserResponse> loadUserCall;
    private Call<UserResponse> updateProfileCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupListeners();
        loadCurrentUserData();
    }

    @Override
    protected void onDestroy() {
        if (loadUserCall != null) loadUserCall.cancel();
        if (updateProfileCall != null) updateProfileCall.cancel();
        super.onDestroy();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnSave.setOnClickListener(v -> handleSaveProfile());
    }

    private void loadCurrentUserData() {
        String authorization = ApiClient.authorization(this);
        if (authorization == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thực hiện chức năng này.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String initialEmail = getIntent().getStringExtra("email");
        String initialDisplayName = getIntent().getStringExtra("display_name");

        if (initialEmail != null && initialDisplayName != null) {
            binding.etEmail.setText(initialEmail);
            binding.etDisplayName.setText(initialDisplayName);
            if (binding.etDisplayName.getText() != null) {
                binding.etDisplayName.setSelection(binding.etDisplayName.getText().length());
            }
            return;
        }

        showLoading(true);
        loadUserCall = ApiClient.auth().getCurrentUser(authorization);
        loadUserCall.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                showLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(EditProfileActivity.this,
                            ApiErrorParser.getMessage(response, getString(R.string.error_profile_load_failed)),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                UserResponse user = response.body();
                binding.etEmail.setText(user.getEmail() == null ? "" : user.getEmail());
                binding.etDisplayName.setText(user.getDisplayName() == null ? "" : user.getDisplayName());
                if (binding.etDisplayName.getText() != null) {
                    binding.etDisplayName.setSelection(binding.etDisplayName.getText().length());
                }
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable throwable) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(EditProfileActivity.this,
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleSaveProfile() {
        String displayName = getText(binding.etDisplayName);

        binding.tilDisplayName.setError(null);

        if (TextUtils.isEmpty(displayName)) {
            binding.tilDisplayName.setError(getString(R.string.error_empty_display_name));
            return;
        }

        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            binding.tilDisplayName.setError(getString(R.string.error_display_name_too_long));
            return;
        }

        String authorization = ApiClient.authorization(this);
        if (authorization == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thực hiện chức năng này.", Toast.LENGTH_LONG).show();
            return;
        }

        showLoading(true);
        updateProfileCall = ApiClient.auth().updateProfile(authorization, new UpdateProfileRequest(displayName));
        updateProfileCall.enqueue(new Callback<UserResponse>() {
            @Override
            public void onResponse(Call<UserResponse> call, Response<UserResponse> response) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                showLoading(false);

                if (!response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this,
                            ApiErrorParser.getMessage(response, getString(R.string.error_update_profile_failed)),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(EditProfileActivity.this,
                        R.string.toast_update_profile_success, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(Call<UserResponse> call, Throwable throwable) {
                if (isFinishing() || isDestroyed() || call.isCanceled()) return;
                showLoading(false);
                Toast.makeText(EditProfileActivity.this,
                        R.string.error_server_unreachable, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.btnSave.setText("");
            binding.btnSave.setEnabled(false);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.btnSave.setText(R.string.btn_save_changes);
            binding.btnSave.setEnabled(true);
            binding.progressBar.setVisibility(View.GONE);
        }
    }

    private String getText(EditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
