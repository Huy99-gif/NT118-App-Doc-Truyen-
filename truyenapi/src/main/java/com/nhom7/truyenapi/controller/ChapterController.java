package com.nhom7.truyenapi.controller;

import com.nhom7.truyenapi.dto.ChapterDetailResponse;
import com.nhom7.truyenapi.service.ChapterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/chapters")
public class ChapterController {
    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    @GetMapping("/batch")
    public List<ChapterDetailResponse> getBatch(@RequestParam(required = false) String ids) {
        return chapterService.getBatch(ids);
    }

    @GetMapping("/{id}")
    public ChapterDetailResponse getById(@PathVariable Long id) {
        return chapterService.getById(id);
    }
}
