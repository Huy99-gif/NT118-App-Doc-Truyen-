package com.nhom7.truyenapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateReadingHistoryRequest(
        @NotNull(message = "storyId không được để trống")
        @Positive(message = "storyId phải lớn hơn 0")
        Long storyId,
        @NotNull(message = "chapterId không được để trống")
        @Positive(message = "chapterId phải lớn hơn 0")
        Long chapterId,
        @NotNull(message = "charOffset không được để trống")
        @Min(value = 0, message = "charOffset không được âm")
        Integer charOffset) {
}
