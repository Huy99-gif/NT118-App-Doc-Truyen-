package com.nhom7.truyenapi.dto;

import java.time.Instant;

public record StorySummaryResponse(
        Long id,
        String title,
        String authorName,
        String coverUrl,
        String status,
        boolean featured,
        Instant updatedAt) {
}
