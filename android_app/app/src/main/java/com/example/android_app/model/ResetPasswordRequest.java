package com.example.android_app.model;

public final class ResetPasswordRequest {
    private final String token;
    private final String newPassword;

    public ResetPasswordRequest(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }
}
