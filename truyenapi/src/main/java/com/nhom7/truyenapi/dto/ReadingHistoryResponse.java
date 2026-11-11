package com.nhom7.truyenapi.dto;

import java.time.Instant;

public record ReadingHistoryResponse(
        Long storyId,
        String storyTitle,
        String coverUrl,
        Long chapterId,
        Integer chapterNo,
        String chapterTitle,
        Integer charOffset,
        Instant updatedAt) {
}
