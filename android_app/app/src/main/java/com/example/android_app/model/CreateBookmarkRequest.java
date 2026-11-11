package com.example.android_app.model;

public class CreateBookmarkRequest {
    public final long chapterId;
    public final int charOffset;
    public final String note;

    public CreateBookmarkRequest(long chapterId, int charOffset, String note) {
        this.chapterId = chapterId;
        this.charOffset = charOffset;
        this.note = note;
    }
}
