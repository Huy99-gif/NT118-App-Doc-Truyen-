package com.nhom7.truyenapi.config;

import com.nhom7.truyenapi.TruyenapiApplication;
import com.nhom7.truyenapi.entity.Author;
import com.nhom7.truyenapi.entity.Chapter;
import com.nhom7.truyenapi.entity.Comment;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TruyenapiApplication.class)
@AutoConfigureMockMvc
class CommentApiIntegrationTests {
    private static final String AUTH_HEADER = "Authorization";
    private static final long MISSING_ID = 999_999_999L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private UserRepository users;
    @Autowired private AuthorRepository authors;
    @Autowired private StoryRepository stories;
    @Autowired private ChapterRepository chapters;
    @Autowired private LibraryRepository libraries;
    @Autowired private ReadingHistoryRepository histories;
    @Autowired private BookmarkRepository bookmarks;
    @Autowired private PasswordResetTokenRepository resetTokens;
    @Autowired private CommentRepository comments;
    @Autowired private RatingRepository ratings;

    private User userA;
    private String authorizationA;
    private String authorizationB;
    private Story story;
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
        userA = createUser("commenter-a-" + suffix + "@example.com");
        User userB = createUser("commenter-b-" + suffix + "@example.com");
        authorizationA = tokenFor(userA);
        authorizationB = tokenFor(userB);

        Author author = new Author();
        author.setName("Comment Author " + suffix);
        author = authors.save(author);

        story = createStory(author, "Comment Story " + suffix);
        chapter = createChapter(story, 1, "Chapter One");
        otherChapter = createChapter(story, 2, "Chapter Two");
    }

    // H2 dùng chung giữa các class test: dọn bình luận để test khác xoá user/chương không dính khoá ngoại
    @AfterEach
    void tearDown() {
        comments.deleteAll();
    }

    @Test
    void listAndCreateRequireAuthentication() throws Exception {
        mockMvc.perform(get("/chapters/{id}/comments", chapter.getId()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/chapters/{id}/comments", chapter.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson("hello")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturnsCommentAndLinksStory() throws Exception {
        postComment(authorizationA, chapter.getId(), "  Chương này hay quá 😍  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(userA.getId()))
                .andExpect(jsonPath("$.displayName").value("Test Reader"))
                .andExpect(jsonPath("$.content").value("Chương này hay quá 😍"))
                .andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.createdAt").exists());

        List<Comment> saved = comments.findAll();
        assertEquals(1, saved.size());
        assertEquals(story.getId(), saved.get(0).getStory().getId());
        assertEquals(chapter.getId(), saved.get(0).getChapter().getId());
    }

    @Test
    void createValidatesContent() throws Exception {
        postComment(authorizationA, chapter.getId(), "   ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Nội dung bình luận không được để trống"));
        postComment(authorizationA, chapter.getId(), "a".repeat(1001))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bình luận không được vượt quá 1000 ký tự"));
        postComment(authorizationA, chapter.getId(), "a".repeat(1000))
                .andExpect(status().isCreated());
    }

    @Test
    void unknownChapterReturnsNotFound() throws Exception {
        mockMvc.perform(get("/chapters/{id}/comments", MISSING_ID).header(AUTH_HEADER, authorizationA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Không tìm thấy chương"));
        postComment(authorizationA, MISSING_ID, "hello")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Không tìm thấy chương"));
    }

    @Test
    void listIsNewestFirstPagedAndScopedToChapter() throws Exception {
        postComment(authorizationA, chapter.getId(), "c1").andExpect(status().isCreated());
        postComment(authorizationB, chapter.getId(), "c2").andExpect(status().isCreated());
        postComment(authorizationA, chapter.getId(), "c3").andExpect(status().isCreated());
        postComment(authorizationA, otherChapter.getId(), "other").andExpect(status().isCreated());

        // dòng cũ không gắn chương: không được hiện ở chương nào
        Comment legacy = new Comment();
        legacy.setUser(userA);
        legacy.setStory(story);
        legacy.setContent("legacy");
        comments.save(legacy);

        mockMvc.perform(get("/chapters/{id}/comments", chapter.getId())
                        .header(AUTH_HEADER, authorizationA)
                        .param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].content").value("c3"))
                .andExpect(jsonPath("$.items[0].mine").value(true))
                .andExpect(jsonPath("$.items[1].content").value("c2"))
                .andExpect(jsonPath("$.items[1].mine").value(false))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/chapters/{id}/comments", chapter.getId())
                        .header(AUTH_HEADER, authorizationA)
                        .param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].content").value("c1"));
    }

    @Test
    void listOfChapterWithoutCommentsIsEmpty() throws Exception {
        mockMvc.perform(get("/chapters/{id}/comments", otherChapter.getId())
                        .header(AUTH_HEADER, authorizationA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private ResultActions postComment(String authorization, long chapterId, String content) throws Exception {
        return mockMvc.perform(post("/chapters/{id}/comments", chapterId)
                .header(AUTH_HEADER, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(commentJson(content)));
    }

    private String commentJson(String content) {
        return "{\"content\":\"" + content + "\"}";
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
        Story s = new Story();
        s.setAuthor(storyAuthor);
        s.setTitle(title);
        s.setSummary("Summary " + title);
        s.setStatus("ONGOING");
        s.setFeatured(false);
        s.setUpdatedAt(Instant.now());
        return stories.save(s);
    }

    private Chapter createChapter(Story parent, int chapterNo, String title) {
        Chapter c = new Chapter();
        c.setStory(parent);
        c.setChapterNo(chapterNo);
        c.setTitle(title);
        c.setContent("Content " + title);
        c.setUpdatedAt(Instant.now());
        return chapters.save(c);
    }
}
