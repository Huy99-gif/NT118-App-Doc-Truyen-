package com.nhom7.truyenapi.config;

import com.nhom7.truyenapi.TruyenapiApplication;
import com.nhom7.truyenapi.entity.Author;
import com.nhom7.truyenapi.entity.Bookmark;
import com.nhom7.truyenapi.entity.Chapter;
import com.nhom7.truyenapi.entity.Library;
import com.nhom7.truyenapi.entity.LibraryId;
import com.nhom7.truyenapi.entity.ReadingHistory;
import com.nhom7.truyenapi.entity.ReadingHistoryId;
import com.nhom7.truyenapi.entity.Story;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.repository.AuthorRepository;
import com.nhom7.truyenapi.repository.BookmarkRepository;
import com.nhom7.truyenapi.repository.ChapterRepository;
import com.nhom7.truyenapi.repository.CommentRepository;
import com.nhom7.truyenapi.repository.LibraryRepository;
import com.nhom7.truyenapi.repository.PasswordResetTokenRepository;
import com.nhom7.truyenapi.repository.RatingRepository;
import com.nhom7.truyenapi.repository.ReadingHistoryRepository;
import com.nhom7.truyenapi.repository.StoryRepository;
import com.nhom7.truyenapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TruyenapiApplication.class)
@AutoConfigureMockMvc
class ReadingApisIntegrationTests {
    private static final String AUTH_HEADER = "Authorization";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository users;

    @Autowired
    private AuthorRepository authors;

    @Autowired
    private StoryRepository stories;

    @Autowired
    private ChapterRepository chapters;

    @Autowired
    private LibraryRepository libraries;

    @Autowired
    private ReadingHistoryRepository histories;

    @Autowired
    private BookmarkRepository bookmarks;

    @Autowired
    private PasswordResetTokenRepository resetTokens;

    @Autowired
    private CommentRepository comments;

    @Autowired
    private RatingRepository ratings;

    private User userA;
    private User userB;
    private String authorizationA;
    private String authorizationB;
    private Author author;
    private Story story;
    private Story otherStory;
    private Chapter chapter;
    private Chapter otherChapter;

    @BeforeEach
    void setUp() {
        comments.deleteAll();
        ratings.deleteAll();
        bookmarks.deleteAll();
        histories.deleteAll();
        libraries.deleteAll();
        resetTokens.deleteAll();
        chapters.deleteAll();
        stories.deleteAll();
        authors.deleteAll();
        users.deleteAll();

        String suffix = UUID.randomUUID().toString();
        userA = createUser("reader-a-" + suffix + "@example.com");
        userB = createUser("reader-b-" + suffix + "@example.com");
        authorizationA = tokenFor(userA);
        authorizationB = tokenFor(userB);

        author = new Author();
        author.setName("Test Author " + suffix);
        author = authors.save(author);

        story = createStory(author, "Test Story " + suffix);
        otherStory = createStory(author, "Other Story " + suffix);
        chapter = createChapter(story, 1, "Chapter One");
        otherChapter = createChapter(otherStory, 1, "Other Chapter");
    }

    @Test
    void everyEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/library"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/library")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libraryJson(story.getId())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/library/{storyId}", story.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/history")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson(story.getId(), chapter.getId(), 1)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/history"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/bookmarks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookmarkJson(chapter.getId(), 1, null)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/bookmarks"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/bookmarks/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void libraryIsIdempotentPagedAndScopedToCurrentUser() throws Exception {
        mockMvc.perform(post("/library")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libraryJson(story.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/library")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libraryJson(story.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/library")
                        .header(AUTH_HEADER, authorizationA)
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].storyId").value(story.getId()))
                .andExpect(jsonPath("$.items[0].addedAt").exists());

        mockMvc.perform(get("/library").header(AUTH_HEADER, authorizationB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(post("/library")
                        .header(AUTH_HEADER, authorizationB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libraryJson(story.getId())))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/library/{storyId}", story.getId())
                        .header(AUTH_HEADER, authorizationA))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/library").header(AUTH_HEADER, authorizationB))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(delete("/library/{storyId}", story.getId())
                        .header(AUTH_HEADER, authorizationA))
                .andExpect(status().isNotFound());
    }

    @Test
    void historyUpsertsByUserAndValidatesChapterStoryPair() throws Exception {
        mockMvc.perform(put("/history")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson(story.getId(), chapter.getId(), 12)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.charOffset").value(12))
                .andExpect(jsonPath("$.chapterId").value(chapter.getId()))
                .andExpect(jsonPath("$.updatedAt").exists());

        mockMvc.perform(put("/history")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson(story.getId(), chapter.getId(), 88)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.charOffset").value(88));

        mockMvc.perform(get("/history")
                        .header(AUTH_HEADER, authorizationA)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].charOffset").value(88));
        mockMvc.perform(get("/history").header(AUTH_HEADER, authorizationB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(put("/history")
                        .header(AUTH_HEADER, authorizationB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson(story.getId(), chapter.getId(), 5)))
                .andExpect(status().isOk());
        ReadingHistory historyA = histories.findById(
                new ReadingHistoryId(userA.getId(), story.getId())).orElseThrow();
        assertEquals(88, historyA.getCharOffset());

        mockMvc.perform(put("/history")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson(story.getId(), otherChapter.getId(), 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        mockMvc.perform(put("/history")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson(story.getId(), chapter.getId(), -1)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/history")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(historyJson(999999L, chapter.getId(), 0)))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookmarksTrimNotesAndUseSameNotFoundForForeignAndMissingIds() throws Exception {
        MvcResult ownResult = mockMvc.perform(post("/bookmarks")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookmarkJson(chapter.getId(), 14, "  ghi chú  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value("ghi chú"))
                .andReturn();
        long ownBookmarkId = responseId(ownResult);

        MvcResult foreignResult = mockMvc.perform(post("/bookmarks")
                        .header(AUTH_HEADER, authorizationB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookmarkJson(chapter.getId(), 20, "của B")))
                .andExpect(status().isCreated())
                .andReturn();
        long foreignBookmarkId = responseId(foreignResult);

        mockMvc.perform(get("/bookmarks")
                        .header(AUTH_HEADER, authorizationA)
                        .param("page", "0")
                        .param("size", "51"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(ownBookmarkId));
        mockMvc.perform(get("/bookmarks").header(AUTH_HEADER, authorizationB))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(foreignBookmarkId));

        MvcResult foreignDelete = mockMvc.perform(delete("/bookmarks/{id}", foreignBookmarkId)
                        .header(AUTH_HEADER, authorizationA))
                .andExpect(status().isNotFound())
                .andReturn();
        MvcResult missingDelete = mockMvc.perform(delete("/bookmarks/999999")
                        .header(AUTH_HEADER, authorizationA))
                .andExpect(status().isNotFound())
                .andReturn();
        assertEquals(jsonError(foreignDelete), jsonError(missingDelete));

        mockMvc.perform(get("/bookmarks").header(AUTH_HEADER, authorizationB))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(delete("/bookmarks/{id}", ownBookmarkId)
                        .header(AUTH_HEADER, authorizationA))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsMissingResourcesAndInvalidBookmarkData() throws Exception {
        mockMvc.perform(post("/library")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libraryJson(999999L)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/library/999999")
                        .header(AUTH_HEADER, authorizationA))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/bookmarks")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookmarkJson(999999L, 0, null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/bookmarks")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookmarkJson(chapter.getId(), -1, null)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/bookmarks")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookmarkJson(chapter.getId(), 0, "x".repeat(501))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/bookmarks/999999")
                        .header(AUTH_HEADER, authorizationA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Không tìm thấy bookmark"));
    }

    @Test
    void enforcesLibraryAndBookmarkLimits() throws Exception {
        List<Story> extraStories = IntStream.range(0, 1000)
                .mapToObj(index -> createUnsavedStory("Limit Story " + index))
                .toList();
        List<Story> savedStories = stories.saveAll(extraStories);
        List<Library> entries = savedStories.stream()
                .map(savedStory -> {
                    Library entry = new Library();
                    entry.setId(new LibraryId(userA.getId(), savedStory.getId()));
                    entry.setUser(userA);
                    entry.setStory(savedStory);
                    return entry;
                })
                .toList();
        libraries.saveAll(entries);

        mockMvc.perform(post("/library")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libraryJson(savedStories.get(0).getId())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/library")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(libraryJson(story.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        List<Bookmark> bookmarkEntries = new ArrayList<>();
        for (int index = 0; index < 500; index++) {
            Bookmark entry = new Bookmark();
            entry.setUser(userA);
            entry.setChapter(chapter);
            entry.setCharOffset(index);
            bookmarkEntries.add(entry);
        }
        bookmarks.saveAll(bookmarkEntries);
        mockMvc.perform(post("/bookmarks")
                        .header(AUTH_HEADER, authorizationA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookmarkJson(chapter.getId(), 0, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void concurrentRequestsDoNotDuplicateLibraryOrHistoryRows() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> firstLibrary = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/library")
                                .header(AUTH_HEADER, authorizationA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(libraryJson(story.getId())))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            Future<Integer> secondLibrary = executor.submit(() -> {
                start.await();
                return mockMvc.perform(post("/library")
                                .header(AUTH_HEADER, authorizationA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(libraryJson(story.getId())))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            start.countDown();
            assertEquals(Set.of(200, 201), Set.of(firstLibrary.get(), secondLibrary.get()));
            assertEquals(1, libraries.countById_UserId(userA.getId()));

            CountDownLatch historyStart = new CountDownLatch(1);
            Future<Integer> firstHistory = executor.submit(() -> {
                historyStart.await();
                return mockMvc.perform(put("/history")
                                .header(AUTH_HEADER, authorizationA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(historyJson(story.getId(), chapter.getId(), 1)))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            Future<Integer> secondHistory = executor.submit(() -> {
                historyStart.await();
                return mockMvc.perform(put("/history")
                                .header(AUTH_HEADER, authorizationA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(historyJson(story.getId(), chapter.getId(), 2)))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            });
            historyStart.countDown();
            assertEquals(200, firstHistory.get());
            assertEquals(200, secondHistory.get());
            assertEquals(1, histories.countById_UserId(userA.getId()));
        } finally {
            executor.shutdownNow();
        }
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("test-password-hash");
        user.setDisplayName("Test Reader");
        return users.save(user);
    }

    private String tokenFor(User user) {
        return "Bearer " + jwtUtil.generateToken(user.getId(), user.getEmail());
    }

    private Story createStory(Author storyAuthor, String title) {
        return stories.save(createUnsavedStory(storyAuthor, title));
    }

    private Story createUnsavedStory(String title) {
        return createUnsavedStory(author, title);
    }

    private Story createUnsavedStory(Author storyAuthor, String title) {
        Story story = new Story();
        story.setAuthor(storyAuthor);
        story.setTitle(title);
        story.setSummary("Summary " + title);
        story.setStatus("ONGOING");
        story.setFeatured(false);
        story.setUpdatedAt(Instant.now());
        return story;
    }

    private Chapter createChapter(Story parent, int chapterNo, String title) {
        Chapter chapter = new Chapter();
        chapter.setStory(parent);
        chapter.setChapterNo(chapterNo);
        chapter.setTitle(title);
        chapter.setContent("Content " + title);
        chapter.setUpdatedAt(Instant.now());
        return chapters.save(chapter);
    }

    private String libraryJson(Long storyId) {
        return "{\"storyId\":" + storyId + "}";
    }

    private String historyJson(Long storyId, Long chapterId, int charOffset) {
        return "{\"storyId\":" + storyId
                + ",\"chapterId\":" + chapterId
                + ",\"charOffset\":" + charOffset + "}";
    }

    private String bookmarkJson(Long chapterId, int charOffset, String note) {
        String noteJson = note == null ? "null" : "\"" + note + "\"";
        return "{\"chapterId\":" + chapterId
                + ",\"charOffset\":" + charOffset
                + ",\"note\":" + noteJson + "}";
    }

    private long responseId(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return Long.parseLong(content.substring(content.indexOf("\"id\":") + 5, content.indexOf(',')));
    }

    private String jsonError(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString();
        return content.substring(content.indexOf("\"error\":") + 9, content.indexOf('}'));
    }
}
