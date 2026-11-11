package com.nhom7.truyenapi.service;

import com.nhom7.truyenapi.dto.BookmarkResponse;
import com.nhom7.truyenapi.dto.CreateBookmarkRequest;
import com.nhom7.truyenapi.dto.PageResponse;
import com.nhom7.truyenapi.entity.Bookmark;
import com.nhom7.truyenapi.entity.Chapter;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.exception.ApiException;
import com.nhom7.truyenapi.repository.BookmarkRepository;
import com.nhom7.truyenapi.repository.ChapterRepository;
import com.nhom7.truyenapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class BookmarkService {
    private static final long MAX_BOOKMARKS = 500;
    private static final String NOT_FOUND_MESSAGE = "Không tìm thấy bookmark";

    private final BookmarkRepository bookmarks;
    private final ChapterRepository chapters;
    private final UserRepository users;
    private final CurrentUserService currentUserService;
    private final PageableFactory pageableFactory;
    private final TransactionTemplate transactionTemplate;

    public BookmarkService(
            BookmarkRepository bookmarks,
            ChapterRepository chapters,
            UserRepository users,
            CurrentUserService currentUserService,
            PageableFactory pageableFactory,
            PlatformTransactionManager transactionManager) {
        this.bookmarks = bookmarks;
        this.chapters = chapters;
        this.users = users;
        this.currentUserService = currentUserService;
        this.pageableFactory = pageableFactory;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public BookmarkResponse create(CreateBookmarkRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        String note = request.note() == null ? null : request.note().trim();

        return transactionTemplate.execute(status -> {
            User user = users.findByIdForUpdate(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
            Chapter chapter = chapters.findById(request.chapterId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy chương"));
            if (bookmarks.countByUser_Id(userId) >= MAX_BOOKMARKS) {
                throw new ApiException(
                        HttpStatus.CONFLICT, "Số bookmark không được vượt quá 500");
            }

            Bookmark bookmark = new Bookmark();
            bookmark.setUser(user);
            bookmark.setChapter(chapter);
            bookmark.setCharOffset(request.charOffset());
            bookmark.setNote(note);
            Bookmark saved = bookmarks.saveAndFlush(bookmark);

            return bookmarks.findItemByIdAndUserId(saved.getId(), userId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tải bookmark vừa tạo"));
        });
    }

    public PageResponse<BookmarkResponse> list(Integer page, Integer size) {
        Long userId = currentUserService.getCurrentUser().getId();
        Pageable pageable = pageableFactory.create(page, size, Sort.unsorted());
        Page<BookmarkResponse> result = bookmarks.findItemsByUserId(userId, pageable);
        return PageResponse.from(result);
    }

    public void remove(Long bookmarkId) {
        Long userId = currentUserService.getCurrentUser().getId();
        if (bookmarks.deleteByIdAndUserId(bookmarkId, userId) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE);
        }
    }
}
