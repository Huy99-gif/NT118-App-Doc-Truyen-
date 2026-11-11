package com.nhom7.truyenapi.config;

import com.nhom7.truyenapi.TruyenapiApplication;
import com.nhom7.truyenapi.entity.Author;
import com.nhom7.truyenapi.entity.Category;
import com.nhom7.truyenapi.entity.Chapter;
import com.nhom7.truyenapi.entity.Story;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.exception.ApiException;
import com.nhom7.truyenapi.repository.AuthorRepository;
import com.nhom7.truyenapi.repository.CategoryRepository;
import com.nhom7.truyenapi.repository.ChapterRepository;
import com.nhom7.truyenapi.repository.StoryRepository;
import com.nhom7.truyenapi.repository.UserRepository;
import com.nhom7.truyenapi.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TruyenapiApplication.class)
@AutoConfigureMockMvc
class StoryChapterIntegrationTests {
    private static final String AUTH_HEADER = "Authorization";
    private static final String ONGOING = "ONGOING";
    private static final String COMPLETED = "COMPLETED";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private String authorization;
    private Author author;
    private Category category;

    @BeforeEach
    void setUp() {
        chapterRepository.deleteAll();
        storyRepository.deleteAll();
        categoryRepository.deleteAll();
        authorRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("story-api-test@example.com");
        user.setPasswordHash("test-password-hash");
        user.setDisplayName("Story API Test");
        user = userRepository.save(user);
        authorization = "Bearer " + jwtUtil.generateToken(user.getId(), user.getEmail());

        author = new Author();
        author.setName("Test Author");
        author = authorRepository.save(author);

        category = new Category();
        category.setName("Test Category");
        category = categoryRepository.save(category);
    }

    @Test
    void allReadEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/stories").param("type", "latest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(get("/stories/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/stories/search"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/stories/1/chapters"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/chapters/1"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/chapters/batch").param("ids", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void currentUserServiceLooksUpPrincipalEmailAndRejectsMissingUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "story-api-test@example.com", null, List.of()));
        try {
            assertEquals("story-api-test@example.com",
                    currentUserService.getCurrentUser().getEmail());

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "missing-user@example.com", null, List.of()));
            ApiException exception = assertThrows(
                    ApiException.class, currentUserService::getCurrentUser);
            assertEquals(401, exception.getStatus().value());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void featuredStoriesReturnOnlyFeaturedItemsOrderedByUpdatedAt() throws Exception {
        Story newerFeatured = createStory("Featured New", ONGOING, true,
                Instant.parse("2025-02-01T00:00:00Z"));
        createStory("Not Featured", ONGOING, false,
                Instant.parse("2025-03-01T00:00:00Z"));
        Story olderFeatured = createStory("Featured Old", COMPLETED, true,
                Instant.parse("2025-01-01T00:00:00Z"));

        mockMvc.perform(get("/stories")
                        .header(AUTH_HEADER, authorization)
                        .param("type", "featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].id").value(newerFeatured.getId()))
                .andExpect(jsonPath("$.items[1].id").value(olderFeatured.getId()));
    }

    @Test
    void latestStoriesCanBeFilteredByCategoryId() throws Exception {
        Story matching = createStory("Matching Story", ONGOING, false,
                Instant.parse("2025-02-01T00:00:00Z"));
        Story withoutCategory = createStory("Other Story", ONGOING, false,
                Instant.parse("2025-03-01T00:00:00Z"));
        withoutCategory.getCategories().clear();
        storyRepository.save(withoutCategory);

        mockMvc.perform(get("/stories")
                        .header(AUTH_HEADER, authorization)
                        .param("type", "latest")
                        .param("category", category.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(matching.getId()));
    }

    @Test
    void storyDetailsReturnDtoWithAuthorAndCategories() throws Exception {
        Story story = createStory("Detail Story", ONGOING, false, Instant.now());

        mockMvc.perform(get("/stories/{id}", story.getId())
                        .header(AUTH_HEADER, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(story.getId()))
                .andExpect(jsonPath("$.authorName").value(author.getName()))
                .andExpect(jsonPath("$.categories[0]").value(category.getName()))
                .andExpect(jsonPath("$.chapters").doesNotExist());
    }

    @Test
    void storySearchFiltersByQueryAuthorIdAndExactStatus() throws Exception {
        Story matching = createStory("Moonlit River", ONGOING, false, Instant.now());
        createStory("Moonlit Castle", COMPLETED, false, Instant.now());
        createStory("Sunny Forest", ONGOING, false, Instant.now());
        Author otherAuthor = new Author();
        otherAuthor.setName("Other Author");
        otherAuthor = authorRepository.save(otherAuthor);
        createStory(otherAuthor, "Moonlit River", ONGOING, false, Instant.now());

        mockMvc.perform(get("/stories/search")
                        .header(AUTH_HEADER, authorization)
                        .param("q", " RIVER ")
                        .param("author", author.getId().toString())
                        .param("status", ONGOING))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(matching.getId()));
    }

    @Test
    void storySearchWithoutQueryStillAppliesStatusFilter() throws Exception {
        Story ongoing = createStory("Ongoing Story", ONGOING, false, Instant.now());
        createStory("Completed Story", COMPLETED, false, Instant.now());

        mockMvc.perform(get("/stories/search")
                        .header(AUTH_HEADER, authorization)
                        .param("status", ONGOING))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(ongoing.getId()));
    }

    @Test
    void searchEscapesLikeWildcardsAndRejectsInvalidStatusAndLongQuery() throws Exception {
        Story literalPercent = createStory("100% Real", ONGOING, false, Instant.now());
        createStory("100X Real", ONGOING, false, Instant.now());
        Story literalUnderscore = createStory("100_ Real", ONGOING, false, Instant.now());
        createStory("100A Real", ONGOING, false, Instant.now());

        mockMvc.perform(get("/stories/search")
                        .header(AUTH_HEADER, authorization)
                        .param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(literalPercent.getId()));

        mockMvc.perform(get("/stories/search")
                        .header(AUTH_HEADER, authorization)
                        .param("q", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(literalUnderscore.getId()));

        mockMvc.perform(get("/stories/search")
                        .header(AUTH_HEADER, authorization)
                        .param("status", "ongoing"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Trạng thái không hợp lệ"));

        mockMvc.perform(get("/stories/search")
                        .header(AUTH_HEADER, authorization)
                        .param("q", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void invalidListParametersReturnJsonBadRequest() throws Exception {
        mockMvc.perform(get("/stories")
                        .header(AUTH_HEADER, authorization)
                        .param("type", "random"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/stories")
                        .header(AUTH_HEADER, authorization)
                        .param("type", "latest")
                        .param("category", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/stories/search")
                        .header(AUTH_HEADER, authorization)
                        .param("author", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/stories")
                        .header(AUTH_HEADER, authorization)
                        .param("type", "latest")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/stories")
                        .header(AUTH_HEADER, authorization)
                        .param("type", "latest")
                        .param("page", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/stories")
                        .header(AUTH_HEADER, authorization)
                        .param("type", "latest")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void oversizedPageSizeIsCappedAtFifty() throws Exception {
        for (int i = 0; i < 51; i++) {
            createStory("Paged Story " + i, ONGOING, false, Instant.now());
        }

        mockMvc.perform(get("/stories")
                        .header(AUTH_HEADER, authorization)
                        .param("type", "latest")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.items.length()").value(50))
                .andExpect(jsonPath("$.totalElements").value(51))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void chapterListOmitsContentAndUsesChapterOrder() throws Exception {
        Story story = createStory("Chapters", ONGOING, false, Instant.now());
        Chapter later = createChapter(story, 2, "Second", "secret chapter text");
        Chapter earlier = createChapter(story, 1, "First", "first chapter text");

        mockMvc.perform(get("/stories/{id}/chapters", story.getId())
                        .header(AUTH_HEADER, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(earlier.getId()))
                .andExpect(jsonPath("$.items[0].chapterNo").value(1))
                .andExpect(jsonPath("$.items[0].title").value("First"))
                .andExpect(jsonPath("$.items[0].updatedAt").exists())
                .andExpect(jsonPath("$.items[0].content").doesNotExist())
                .andExpect(jsonPath("$.items[1].id").value(later.getId()))
                .andExpect(jsonPath("$.items[1].content").doesNotExist());
    }

    @Test
    void chapterDetailsReturnContent() throws Exception {
        Story story = createStory("Chapter Detail", ONGOING, false, Instant.now());
        Chapter chapter = createChapter(story, 1, "Chapter One", "Full chapter content");

        mockMvc.perform(get("/chapters/{id}", chapter.getId())
                        .header(AUTH_HEADER, authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(chapter.getId()))
                .andExpect(jsonPath("$.storyId").value(story.getId()))
                .andExpect(jsonPath("$.content").value("Full chapter content"));
    }

    @Test
    void batchRemovesDuplicatesIgnoresMissingIdsAndPreservesRequestOrder() throws Exception {
        Story story = createStory("Batch", ONGOING, false, Instant.now());
        Chapter first = createChapter(story, 1, "First", "First content");
        Chapter second = createChapter(story, 2, "Second", "Second content");

        mockMvc.perform(get("/chapters/batch")
                        .header(AUTH_HEADER, authorization)
                        .param("ids", second.getId() + "," + first.getId() + ","
                                + second.getId() + ",999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second.getId()))
                .andExpect(jsonPath("$[1].id").value(first.getId()))
                .andExpect(jsonPath("$[0].content").value("Second content"));
    }

    @Test
    void batchRejectsMoreThanTwentyIdsAndNonnumericIds() throws Exception {
        String tooManyIds = LongStream.rangeClosed(1, 21)
                .mapToObj(Long::toString)
                .collect(Collectors.joining(","));
        mockMvc.perform(get("/chapters/batch")
                        .header(AUTH_HEADER, authorization)
                        .param("ids", tooManyIds))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/chapters/batch")
                        .header(AUTH_HEADER, authorization)
                        .param("ids", "1,not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void missingStoryOrChapterReturnsJsonNotFound() throws Exception {
        mockMvc.perform(get("/stories/999999")
                        .header(AUTH_HEADER, authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(get("/stories/999999/chapters")
                        .header(AUTH_HEADER, authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(get("/chapters/999999")
                        .header(AUTH_HEADER, authorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    private Story createStory(String title, String status, boolean featured, Instant updatedAt) {
        return createStory(author, title, status, featured, updatedAt);
    }

    private Story createStory(
            Author storyAuthor, String title, String status, boolean featured, Instant updatedAt) {
        Story story = new Story();
        story.setAuthor(storyAuthor);
        story.setTitle(title);
        story.setSummary("Summary for " + title);
        story.setStatus(status);
        story.setFeatured(featured);
        story.setUpdatedAt(updatedAt);
        story.getCategories().add(category);
        return storyRepository.save(story);
    }

    private Chapter createChapter(Story story, int chapterNo, String title, String content) {
        Chapter chapter = new Chapter();
        chapter.setStory(story);
        chapter.setChapterNo(chapterNo);
        chapter.setTitle(title);
        chapter.setContent(content);
        chapter.setUpdatedAt(Instant.now());
        return chapterRepository.save(chapter);
    }
}
