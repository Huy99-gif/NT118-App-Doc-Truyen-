package com.nhom7.truyenapi.service;

import com.nhom7.truyenapi.dto.PageResponse;
import com.nhom7.truyenapi.dto.StoryDetailResponse;
import com.nhom7.truyenapi.dto.StorySummaryResponse;
import com.nhom7.truyenapi.entity.Story;
import com.nhom7.truyenapi.exception.ApiException;
import com.nhom7.truyenapi.repository.StoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

@Service
public class StoryService {
    private static final Set<String> VALID_STATUSES = Set.of("ONGOING", "COMPLETED");
    private static final Sort STORY_SORT = Sort.by(
                    Sort.Order.desc("updatedAt"),
                    Sort.Order.asc("title"));

    private final StoryRepository stories;
    private final PageableFactory pageableFactory;

    public StoryService(StoryRepository stories, PageableFactory pageableFactory) {
        this.stories = stories;
        this.pageableFactory = pageableFactory;
    }

    public PageResponse<StorySummaryResponse> list(
            String type, String category, Integer page, Integer size) {
        if (!"featured".equals(type) && !"latest".equals(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Loại danh sách không hợp lệ");
        }
        Long categoryId = parseOptionalId("category", category);
        Page<StorySummaryResponse> result = stories.findStories(
                "featured".equals(type),
                categoryId,
                false,
                "",
                null,
                null,
                pageableFactory.create(page, size, STORY_SORT));
        return PageResponse.from(result);
    }

    public StoryDetailResponse getById(Long id) {
        Story story = stories.findDetailedById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện"));
        return new StoryDetailResponse(
                story.getId(),
                story.getTitle(),
                story.getAuthor().getName(),
                story.getCoverUrl(),
                story.getSummary(),
                story.getStatus(),
                story.isFeatured(),
                story.getUpdatedAt(),
                story.getCategories().stream()
                        .map(category -> category.getName())
                        .sorted()
                        .toList());
    }

    public PageResponse<StorySummaryResponse> search(
            String query, String author, String status, Integer page, Integer size) {
        validateLength("q", query);
        validateLength("author", author);
        if (status != null && !VALID_STATUSES.contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ");
        }

        boolean hasQuery = query != null && !query.isBlank();
        String queryPattern = hasQuery
                ? "%" + escapeLikeWildcards(query.trim().toLowerCase(Locale.ROOT)) + "%"
                : "";
        Long authorId = parseOptionalId("author", author);
        Page<StorySummaryResponse> result = stories.findStories(
                false,
                null,
                hasQuery,
                queryPattern,
                authorId,
                status,
                pageableFactory.create(page, size, STORY_SORT));
        return PageResponse.from(result);
    }

    private void validateLength(String name, String value) {
        if (value != null && value.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, name + " không được vượt quá 100 ký tự");
        }
    }

    private Long parseOptionalId(String name, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, name + " phải là số");
        }
    }

    private String escapeLikeWildcards(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
