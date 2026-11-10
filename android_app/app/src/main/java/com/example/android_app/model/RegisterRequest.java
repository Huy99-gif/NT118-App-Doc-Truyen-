package com.example.android_app.model;

public final class RegisterRequest {
    private final String email;
    private final String password;
    private final String displayName;

    public RegisterRequest(String email, String password, String displayName) {
        this.email = email;
        this.password = password;
        this.displayName = displayName;
    }
}
