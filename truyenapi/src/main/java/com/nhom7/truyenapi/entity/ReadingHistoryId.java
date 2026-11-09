package com.nhom7.truyenapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ReadingHistoryId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "story_id")
    private Long storyId;

    public ReadingHistoryId() {}

    public ReadingHistoryId(Long userId, Long storyId) {
        this.userId = userId;
        this.storyId = storyId;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getStoryId() { return storyId; }
    public void setStoryId(Long storyId) { this.storyId = storyId; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ReadingHistoryId historyId)) return false;
        return Objects.equals(userId, historyId.userId)
                && Objects.equals(storyId, historyId.storyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, storyId);
    }
}
