package com.nhom7.truyenapi.controller;

import com.nhom7.truyenapi.dto.PageResponse;
import com.nhom7.truyenapi.dto.ReadingHistoryResponse;
import com.nhom7.truyenapi.dto.UpdateReadingHistoryRequest;
import com.nhom7.truyenapi.service.ReadingHistoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/history")
public class HistoryController {
    private final ReadingHistoryService historyService;

    public HistoryController(ReadingHistoryService historyService) {
        this.historyService = historyService;
    }

    @PutMapping
    public ReadingHistoryResponse update(@Valid @RequestBody UpdateReadingHistoryRequest request) {
        return historyService.update(request);
    }

    @GetMapping
    public PageResponse<ReadingHistoryResponse> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return historyService.list(page, size);
    }
}
