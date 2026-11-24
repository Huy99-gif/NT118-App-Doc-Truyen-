package com.nhom7.truyenapi.dto;

import java.time.Instant;

// mine = bình luận của user đang gọi api (client dùng để hiện nút xoá)
public record CommentResponse(
        Long id,
        Long userId,
        String displayName,
        String content,
        Instant createdAt,
        Boolean mine) {
}
