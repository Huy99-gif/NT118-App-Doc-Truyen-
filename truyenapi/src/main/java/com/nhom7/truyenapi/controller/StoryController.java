package com.nhom7.truyenapi.controller;

import com.nhom7.truyenapi.dto.ChapterSummaryResponse;
import com.nhom7.truyenapi.dto.PageResponse;
import com.nhom7.truyenapi.dto.StoryDetailResponse;
import com.nhom7.truyenapi.dto.StorySummaryResponse;
import com.nhom7.truyenapi.service.ChapterService;
import com.nhom7.truyenapi.service.StoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stories")
public class StoryController {
    private final StoryService storyService;
    private final ChapterService chapterService;

    public StoryController(StoryService storyService, ChapterService chapterService) {
        this.storyService = storyService;
        this.chapterService = chapterService;
    }

    @GetMapping
    public PageResponse<StorySummaryResponse> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return storyService.list(type, category, page, size);
    }

    @GetMapping("/search")
    public PageResponse<StorySummaryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return storyService.search(q, author, status, page, size);
    }

    @GetMapping("/{id}")
    public StoryDetailResponse getById(@PathVariable Long id) {
        return storyService.getById(id);
    }

    @GetMapping("/{id}/chapters")
    public PageResponse<ChapterSummaryResponse> listChapters(
            @PathVariable Long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return chapterService.listByStory(id, page, size);
    }
}
