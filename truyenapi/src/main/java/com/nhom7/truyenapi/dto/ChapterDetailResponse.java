package com.nhom7.truyenapi.dto;

import java.time.Instant;

public record ChapterDetailResponse(
        Long id,
        Long storyId,
        Integer chapterNo,
        String title,
        String content,
        Instant updatedAt) {
}
