package com.nhom7.truyenapi.dto;

import java.time.Instant;

public record ChapterSummaryResponse(
        Long id,
        Integer chapterNo,
        String title,
        Instant updatedAt) {
}
