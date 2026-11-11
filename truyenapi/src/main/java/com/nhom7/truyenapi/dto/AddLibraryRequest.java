package com.nhom7.truyenapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddLibraryRequest(
        @NotNull(message = "storyId không được để trống")
        @Positive(message = "storyId phải lớn hơn 0")
        Long storyId) {
}
