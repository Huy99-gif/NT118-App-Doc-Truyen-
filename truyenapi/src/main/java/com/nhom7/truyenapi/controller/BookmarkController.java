package com.nhom7.truyenapi.controller;

import com.nhom7.truyenapi.dto.BookmarkResponse;
import com.nhom7.truyenapi.dto.CreateBookmarkRequest;
import com.nhom7.truyenapi.dto.PageResponse;
import com.nhom7.truyenapi.service.BookmarkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookmarks")
public class BookmarkController {
    private final BookmarkService bookmarkService;

    public BookmarkController(BookmarkService bookmarkService) {
        this.bookmarkService = bookmarkService;
    }

    @PostMapping
    public ResponseEntity<BookmarkResponse> create(
            @Valid @RequestBody CreateBookmarkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookmarkService.create(request));
    }

    @GetMapping
    public PageResponse<BookmarkResponse> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return bookmarkService.list(page, size);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        bookmarkService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
