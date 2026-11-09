package com.example.android_app.model;

public class Chapter {
    private String id;
    private int chapterNumber;
    private String title;
    private String updatedDate;
    private int viewCount;

    public Chapter(String id, int chapterNumber, String title, String updatedDate, int viewCount) {
        this.id = id;
        this.chapterNumber = chapterNumber;
        this.title = title;
        this.updatedDate = updatedDate;
        this.viewCount = viewCount;
    }

    public String getId() {
        return id;
    }

    public int getChapterNumber() {
        return chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public int getViewCount() {
        return viewCount;
    }
}
