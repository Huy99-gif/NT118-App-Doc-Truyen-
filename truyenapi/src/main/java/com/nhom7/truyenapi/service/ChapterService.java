package com.nhom7.truyenapi.service;

import com.nhom7.truyenapi.dto.ChapterDetailResponse;
import com.nhom7.truyenapi.dto.ChapterSummaryResponse;
import com.nhom7.truyenapi.dto.PageResponse;
import com.nhom7.truyenapi.exception.ApiException;
import com.nhom7.truyenapi.repository.ChapterRepository;
import com.nhom7.truyenapi.repository.StoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChapterService {
    private static final int MAX_BATCH_SIZE = 20;

    private final ChapterRepository chapters;
    private final StoryRepository stories;
    private final PageableFactory pageableFactory;

    public ChapterService(
            ChapterRepository chapters,
            StoryRepository stories,
            PageableFactory pageableFactory) {
        this.chapters = chapters;
        this.stories = stories;
        this.pageableFactory = pageableFactory;
    }

    public PageResponse<ChapterSummaryResponse> listByStory(
            Long storyId, Integer page, Integer size) {
        if (!stories.existsById(storyId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện");
        }
        Page<ChapterSummaryResponse> result = chapters.findSummariesByStoryId(
                storyId,
                pageableFactory.create(page, size, Sort.unsorted()));
        return PageResponse.from(result);
    }

    public ChapterDetailResponse getById(Long id) {
        return chapters.findDetailById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy chương"));
    }

    public List<ChapterDetailResponse> getBatch(String ids) {
        if (ids == null || ids.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Danh sách id không được để trống");
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (String part : ids.split(",", -1)) {
            try {
                uniqueIds.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException exception) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Danh sách id không hợp lệ");
            }
        }
        if (uniqueIds.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Danh sách id không được để trống");
        }
        if (uniqueIds.size() > MAX_BATCH_SIZE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mỗi lần chỉ được tải tối đa 20 chương");
        }

        Map<Long, ChapterDetailResponse> foundById = chapters.findDetailsByIdIn(uniqueIds).stream()
                .collect(Collectors.toMap(ChapterDetailResponse::id, chapter -> chapter));
        List<ChapterDetailResponse> orderedResults = new ArrayList<>();
        for (Long id : uniqueIds) {
            ChapterDetailResponse chapter = foundById.get(id);
            if (chapter != null) {
                orderedResults.add(chapter);
            }
        }
        return orderedResults;
    }
}
