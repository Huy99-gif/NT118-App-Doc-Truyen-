package com.nhom7.truyenapi.controller;

import com.nhom7.truyenapi.dto.RatingSummary;
import com.nhom7.truyenapi.entity.Rating;
import com.nhom7.truyenapi.entity.Story;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.exception.ApiException;
import com.nhom7.truyenapi.repository.RatingRepository;
import com.nhom7.truyenapi.repository.StoryRepository;
import com.nhom7.truyenapi.service.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/stories/{storyId}/rating")
public class RatingController {
    public record Vote(@NotNull @Min(1) @Max(5) @Digits(integer = 1, fraction = 0) BigDecimal stars) {}

    private final StoryRepository stories;
    private final RatingRepository ratings;
    private final CurrentUserService users;

    public RatingController(StoryRepository stories, RatingRepository ratings, CurrentUserService users) {
        this.stories = stories;
        this.ratings = ratings;
        this.users = users;
    }

    @GetMapping
    public RatingSummary get(@PathVariable Long storyId) {
        User user = users.getCurrentUser();
        if (!stories.existsById(storyId)) throw missingStory();
        return ratings.summary(storyId, user.getId());
    }

    @PutMapping
    @Transactional
    public RatingSummary vote(@PathVariable Long storyId, @Valid @RequestBody Vote vote) {
        User user = users.getCurrentUser();
        Story story = stories.lockForRating(storyId).orElseThrow(this::missingStory);
        Rating rating = ratings.findByStoryIdAndUserId(storyId, user.getId()).orElseGet(Rating::new);
        rating.setStory(story);
        rating.setUser(user);
        rating.setStars(vote.stars().shortValueExact());
        ratings.saveAndFlush(rating);
        return ratings.summary(storyId, user.getId());
    }

    private ApiException missingStory() {
        return new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy truyện");
    }
}
