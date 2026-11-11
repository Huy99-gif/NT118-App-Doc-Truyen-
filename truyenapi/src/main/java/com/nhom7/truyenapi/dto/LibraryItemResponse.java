package com.nhom7.truyenapi.dto;

import java.time.Instant;

public record LibraryItemResponse(
        Long storyId,
        String title,
        String authorName,
        String coverUrl,
        String status,
        boolean featured,
        Instant updatedAt,
        Instant addedAt) {
}
