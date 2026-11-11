package com.nhom7.truyenapi.service;

import com.nhom7.truyenapi.dto.LibraryItemResponse;
import com.nhom7.truyenapi.dto.PageResponse;
import com.nhom7.truyenapi.entity.Library;
import com.nhom7.truyenapi.entity.LibraryId;
import com.nhom7.truyenapi.entity.Story;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.exception.ApiException;
import com.nhom7.truyenapi.repository.LibraryRepository;
import com.nhom7.truyenapi.repository.StoryRepository;
import com.nhom7.truyenapi.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class LibraryService {
    private static final long MAX_LIBRARY_SIZE = 1000;

    private final LibraryRepository libraries;
    private final StoryRepository stories;
    private final UserRepository users;
    private final CurrentUserService currentUserService;
    private final PageableFactory pageableFactory;
    private final TransactionTemplate transactionTemplate;

    public LibraryService(
            LibraryRepository libraries,
            StoryRepository stories,
            UserRepository users,
            CurrentUserService currentUserService,
            PageableFactory pageableFactory,
            PlatformTransactionManager transactionManager) {
        this.libraries = libraries;
        this.stories = stories;
        this.users = users;
        this.currentUserService = currentUserService;
        this.pageableFactory = pageableFactory;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public PageResponse<LibraryItemResponse> list(Integer page, Integer size) {
        Long userId = currentUserService.getCurrentUser().getId();
        Pageable pageable = pageableFactory.create(page, size, Sort.unsorted());
        Page<LibraryItemResponse> result = libraries.findItemsByUserId(userId, pageable);
        return PageResponse.from(result);
    }

    public boolean add(Long storyId) {
        Long userId = currentUserService.getCurrentUser().getId();
        LibraryId libraryId = new LibraryId(userId, storyId);
        try {
            return transactionTemplate.execute(status -> {
                User user = lockCurrentUser(userId);
                Story story = stories.findById(storyId)
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND, "Không tìm thấy truyện"));

                if (libraries.existsById(libraryId)) {
                    return false;
                }
                if (libraries.countById_UserId(userId) >= MAX_LIBRARY_SIZE) {
                    throw new ApiException(
                            HttpStatus.CONFLICT, "Tủ sách không được vượt quá 1000 truyện");
                }

                Library library = new Library();
                library.setId(libraryId);
                library.setUser(user);
                library.setStory(story);
                libraries.saveAndFlush(library);
                return true;
            });
        } catch (DataIntegrityViolationException exception) {
            if (libraries.existsById(libraryId)) {
                return false;
            }
            throw exception;
        }
    }

    public void remove(Long storyId) {
        Long userId = currentUserService.getCurrentUser().getId();
        int deleted = libraries.deleteByUserIdAndStoryId(userId, storyId);
        if (deleted == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện trong tủ sách");
        }
    }

    private User lockCurrentUser(Long userId) {
        return users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
    }
}
