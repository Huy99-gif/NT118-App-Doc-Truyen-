package com.example.android_app.model;

public class Story {
    private String id;
    private String name;
    private String author;
    private String coverUrl;
    private String latestChapter;
    private int chapterCount;
    private boolean isReading;
    private String status;
    private long initialChapterId;
    private int charOffset;

    public Story(String id, String name, String author, String coverUrl, String latestChapter, int chapterCount) {
        this.id = id;
        this.name = name;
        this.author = author;
        this.coverUrl = coverUrl;
        this.latestChapter = latestChapter;
        this.chapterCount = chapterCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAuthor() { return author; }
    public String getCoverUrl() { return coverUrl; }
    public String getLatestChapter() { return latestChapter; }
    public int getChapterCount() { return chapterCount; }
    public boolean isReading() { return isReading; }
    public void setReading(boolean reading) { isReading = reading; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public long getInitialChapterId() { return initialChapterId; }
    public void setInitialChapterId(long initialChapterId) { this.initialChapterId = initialChapterId; }
    public int getCharOffset() { return charOffset; }
    public void setCharOffset(int charOffset) { this.charOffset = charOffset; }
}
