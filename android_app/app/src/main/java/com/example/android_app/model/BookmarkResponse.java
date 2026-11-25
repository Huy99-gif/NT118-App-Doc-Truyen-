package com.example.android_app.model;

public class BookmarkResponse {
    public Long id;
    public Long storyId;
    public String storyTitle;
    public String coverUrl;
    public Long chapterId;
    public Integer chapterNo;
    public String chapterTitle;
    public Integer charOffset;
    public String note;
    /** Dấu trang tạo lúc offline, đang chờ gửi lên server (không đến từ API). */
    public transient boolean pending;
    public transient long localId;
}
