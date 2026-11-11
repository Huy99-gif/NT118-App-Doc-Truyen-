package com.nhom7.truyenapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateBookmarkRequest(
        @NotNull(message = "chapterId không được để trống")
        @Positive(message = "chapterId phải lớn hơn 0")
        Long chapterId,
        @NotNull(message = "charOffset không được để trống")
        @Min(value = 0, message = "charOffset không được âm")
        Integer charOffset,
        @Size(max = 500, message = "note không được vượt quá 500 ký tự")
        String note) {
}
