package com.nhom7.truyenapi.controller;

import com.nhom7.truyenapi.dto.CommentResponse;
import com.nhom7.truyenapi.dto.CreateCommentRequest;
import com.nhom7.truyenapi.dto.PageResponse;
import com.nhom7.truyenapi.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/chapters/{chapterId}/comments")
    public PageResponse<CommentResponse> list(
            @PathVariable Long chapterId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return commentService.list(chapterId, page, size);
    }

    @PostMapping("/chapters/{chapterId}/comments")
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long chapterId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.create(chapterId, request));
    }

    @GetMapping("/stories/{storyId}/comments")
    public PageResponse<CommentResponse> listStory(
            @PathVariable Long storyId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return commentService.listStory(storyId, page, size);
    }

    @PostMapping("/stories/{storyId}/comments")
    public ResponseEntity<CommentResponse> createStory(
            @PathVariable Long storyId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createStory(storyId, request));
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        commentService.remove(id);
        return ResponseEntity.noContent().build();
    }
}
