package com.nhom7.truyenapi.service;

import com.nhom7.truyenapi.dto.CommentResponse;
import com.nhom7.truyenapi.dto.CreateCommentRequest;
import com.nhom7.truyenapi.dto.PageResponse;
import com.nhom7.truyenapi.entity.Chapter;
import com.nhom7.truyenapi.entity.Comment;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.exception.ApiException;
import com.nhom7.truyenapi.repository.ChapterRepository;
import com.nhom7.truyenapi.repository.CommentRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {
    private static final String CHAPTER_NOT_FOUND = "Không tìm thấy chương";

    private final CommentRepository comments;
    private final ChapterRepository chapters;
    private final CurrentUserService currentUserService;
    private final PageableFactory pageableFactory;

    public CommentService(
            CommentRepository comments,
            ChapterRepository chapters,
            CurrentUserService currentUserService,
            PageableFactory pageableFactory) {
        this.comments = comments;
        this.chapters = chapters;
        this.currentUserService = currentUserService;
        this.pageableFactory = pageableFactory;
    }

    public PageResponse<CommentResponse> list(Long chapterId, Integer page, Integer size) {
        Long userId = currentUserService.getCurrentUser().getId();
        if (!chapters.existsById(chapterId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, CHAPTER_NOT_FOUND);
        }
        // thứ tự mới nhất trước đã nằm trong query nên không truyền sort
        Pageable pageable = pageableFactory.create(page, size, Sort.unsorted());
        return PageResponse.from(comments.findItemsByChapterId(chapterId, userId, pageable));
    }

    @Transactional
    public CommentResponse create(Long chapterId, CreateCommentRequest request) {
        User user = currentUserService.getCurrentUser();
        Chapter chapter = chapters.findById(chapterId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, CHAPTER_NOT_FOUND));

        Comment comment = new Comment();
        comment.setUser(user);
        comment.setChapter(chapter);
        // giữ story_id (NOT NULL) để sau này đếm bình luận theo truyện được
        comment.setStory(chapter.getStory());
        comment.setContent(request.content().trim());
        Comment saved = comments.saveAndFlush(comment);

        return comments.findItemById(saved.getId(), user.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tải bình luận vừa tạo"));
    }
}
