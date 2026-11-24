package com.nhom7.truyenapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Nội dung bình luận không được để trống")
        @Size(max = 1000, message = "Bình luận không được vượt quá 1000 ký tự")
        String content) {
}
