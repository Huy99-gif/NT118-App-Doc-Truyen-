package com.nhom7.truyenapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reading_history")
public class ReadingHistory {
    @EmbeddedId
    private ReadingHistoryId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("storyId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @Column(name = "char_offset")
    private Integer charOffset;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public ReadingHistoryId getId() { return id; }
    public void setId(ReadingHistoryId id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Story getStory() { return story; }
    public void setStory(Story story) { this.story = story; }
    public Chapter getChapter() { return chapter; }
    public void setChapter(Chapter chapter) { this.chapter = chapter; }
    public Integer getCharOffset() { return charOffset; }
    public void setCharOffset(Integer charOffset) { this.charOffset = charOffset; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    void onCreate() {
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
