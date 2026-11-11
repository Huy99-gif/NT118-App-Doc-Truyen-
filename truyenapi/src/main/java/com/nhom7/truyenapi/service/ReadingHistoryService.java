package com.nhom7.truyenapi.service;

import com.nhom7.truyenapi.dto.PageResponse;
import com.nhom7.truyenapi.dto.ReadingHistoryResponse;
import com.nhom7.truyenapi.dto.UpdateReadingHistoryRequest;
import com.nhom7.truyenapi.entity.Chapter;
import com.nhom7.truyenapi.entity.ReadingHistory;
import com.nhom7.truyenapi.entity.ReadingHistoryId;
import com.nhom7.truyenapi.entity.Story;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.exception.ApiException;
import com.nhom7.truyenapi.repository.ChapterRepository;
import com.nhom7.truyenapi.repository.ReadingHistoryRepository;
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

import java.time.Instant;

@Service
public class ReadingHistoryService {
    private final ReadingHistoryRepository histories;
    private final StoryRepository stories;
    private final ChapterRepository chapters;
    private final UserRepository users;
    private final CurrentUserService currentUserService;
    private final PageableFactory pageableFactory;
    private final TransactionTemplate transactionTemplate;

    public ReadingHistoryService(
            ReadingHistoryRepository histories,
            StoryRepository stories,
            ChapterRepository chapters,
            UserRepository users,
            CurrentUserService currentUserService,
            PageableFactory pageableFactory,
            PlatformTransactionManager transactionManager) {
        this.histories = histories;
        this.stories = stories;
        this.chapters = chapters;
        this.users = users;
        this.currentUserService = currentUserService;
        this.pageableFactory = pageableFactory;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ReadingHistoryResponse update(UpdateReadingHistoryRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        ReadingHistoryId historyId = new ReadingHistoryId(userId, request.storyId());
        try {
            return transactionTemplate.execute(status ->
                    saveHistory(userId, historyId, request));
        } catch (DataIntegrityViolationException exception) {
            if (!histories.existsById(historyId)) {
                throw exception;
            }
            return transactionTemplate.execute(status ->
                    saveHistory(userId, historyId, request));
        }
    }

    public PageResponse<ReadingHistoryResponse> list(Integer page, Integer size) {
        Long userId = currentUserService.getCurrentUser().getId();
        Pageable pageable = pageableFactory.create(page, size, Sort.unsorted());
        Page<ReadingHistoryResponse> result = histories.findItemsByUserId(userId, pageable);
        return PageResponse.from(result);
    }

    private ReadingHistoryResponse saveHistory(
            Long userId,
            ReadingHistoryId historyId,
            UpdateReadingHistoryRequest request) {
        User user = users.findByIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập"));
        Story story = stories.findById(request.storyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện"));
        Chapter chapter = chapters.findByIdAndStory_Id(request.chapterId(), request.storyId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST, "Chương không thuộc truyện đã chọn"));

        ReadingHistory history = histories.findById(historyId).orElseGet(ReadingHistory::new);
        history.setId(historyId);
        history.setUser(user);
        history.setStory(story);
        history.setChapter(chapter);
        history.setCharOffset(request.charOffset());
        history.setUpdatedAt(Instant.now());
        histories.saveAndFlush(history);

        return new ReadingHistoryResponse(
                story.getId(),
                story.getTitle(),
                story.getCoverUrl(),
                chapter.getId(),
                chapter.getChapterNo(),
                chapter.getTitle(),
                history.getCharOffset(),
                history.getUpdatedAt());
    }
}
