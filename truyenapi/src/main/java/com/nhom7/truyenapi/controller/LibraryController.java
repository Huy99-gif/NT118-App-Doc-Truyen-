package com.nhom7.truyenapi.controller;

import com.nhom7.truyenapi.dto.AddLibraryRequest;
import com.nhom7.truyenapi.dto.LibraryItemResponse;
import com.nhom7.truyenapi.dto.PageResponse;
import com.nhom7.truyenapi.service.LibraryService;
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
@RequestMapping("/library")
public class LibraryController {
    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping
    public PageResponse<LibraryItemResponse> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return libraryService.list(page, size);
    }

    @PostMapping
    public ResponseEntity<Void> add(@Valid @RequestBody AddLibraryRequest request) {
        boolean created = libraryService.add(request.storyId());
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK).build();
    }

    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> remove(@PathVariable Long storyId) {
        libraryService.remove(storyId);
        return ResponseEntity.noContent().build();
    }
}
