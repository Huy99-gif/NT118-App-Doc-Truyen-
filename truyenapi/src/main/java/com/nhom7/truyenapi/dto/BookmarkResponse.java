package com.nhom7.truyenapi.dto;

public record BookmarkResponse(
        Long id,
        Long storyId,
        String storyTitle,
        String coverUrl,
        Long chapterId,
        Integer chapterNo,
        String chapterTitle,
        Integer charOffset,
        String note) {
}
