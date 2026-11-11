package com.example.android_app.model;

public class UpdateReadingHistoryRequest {
    public final long storyId;
    public final long chapterId;
    public final int charOffset;

    public UpdateReadingHistoryRequest(long storyId, long chapterId, int charOffset) {
        this.storyId = storyId;
        this.chapterId = chapterId;
        this.charOffset = charOffset;
    }
}
