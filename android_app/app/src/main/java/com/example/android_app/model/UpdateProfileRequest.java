package com.example.android_app.model;

public class UpdateProfileRequest {
    private String displayName;

    public UpdateProfileRequest() {}

    public UpdateProfileRequest(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
