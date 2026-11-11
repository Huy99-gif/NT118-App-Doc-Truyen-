package com.nhom7.truyenapi.dto;

import java.time.Instant;
import java.util.List;

public record StoryDetailResponse(
        Long id,
        String title,
        String authorName,
        String coverUrl,
        String summary,
        String status,
        boolean featured,
        Instant updatedAt,
        List<String> categories) {
}
