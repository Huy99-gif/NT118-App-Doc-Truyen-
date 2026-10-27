package com.nhom7.truyenapi.config;

import com.nhom7.truyenapi.TruyenapiApplication;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.exception.ApiExceptionHandler;
import com.nhom7.truyenapi.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TruyenapiApplication.class)
@AutoConfigureMockMvc
class AuthIntegrationTests {
    private static final String REGISTER_URL = "/auth/register";
    private static final String LOGIN_URL = "/auth/login";
    private static final String ME_URL = "/auth/me";
    private static final String TEST_EMAIL = "auth-test@example.com";
    private static final String TEST_PASSWORD = "MatKhau123";
    private final JsonMapper objectMapper = JsonMapper.builder().build();
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiExceptionHandler apiExceptionHandler;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void registerReturnsCreatedAndDoesNotExposePassword() throws Exception {
        MvcResult result = register(TEST_EMAIL, TEST_PASSWORD, "Test Reader")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andReturn();

        assertResponseDoesNotExposePassword(result);
        User savedUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
        assertFalse(savedUser.getPasswordHash().equals(TEST_PASSWORD));
    }

    @Test
    void registerDuplicateEmailReturnsConflict() throws Exception {
        register(TEST_EMAIL, TEST_PASSWORD, "Test Reader")
                .andExpect(status().isCreated());

        register(TEST_EMAIL, TEST_PASSWORD, "Another Reader")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email đã tồn tại"));
    }

    @Test
    void registerDuplicateEmailWithDifferentCaseReturnsConflict() throws Exception {
        register("Reader@Example.com", TEST_PASSWORD, "Test Reader")
                .andExpect(status().isCreated());

        register(" reader@example.COM ", TEST_PASSWORD, "Another Reader")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email đã tồn tại"));
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        register("not-an-email", TEST_PASSWORD, "Test Reader")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registerRejectsBlankEmail() throws Exception {
        register("   ", TEST_PASSWORD, "Test Reader")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registerRejectsPasswordShorterThanSixCharacters() throws Exception {
        register(TEST_EMAIL, "abc12", "Test Reader")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void registerRejectsBlankDisplayName() throws Exception {
        register(TEST_EMAIL, TEST_PASSWORD, " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void loginReturnsJwtAndDoesNotExposePassword() throws Exception {
        register(TEST_EMAIL, TEST_PASSWORD, "Test Reader")
                .andExpect(status().isCreated());

        MvcResult result = login(TEST_EMAIL, TEST_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.displayName").value("Test Reader"))
                .andReturn();

        assertResponseDoesNotExposePassword(result);
    }

    @Test
    void loginRejectsIncorrectPassword() throws Exception {
        register(TEST_EMAIL, TEST_PASSWORD, "Test Reader")
                .andExpect(status().isCreated());

        login(TEST_EMAIL, "WrongPassword")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Sai email hoặc mật khẩu"));
    }

    @Test
    void loginRejectsUnknownEmailWithGenericCredentialsError() throws Exception {
        login("unknown@example.com", TEST_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Sai email hoặc mật khẩu"));
    }

    @Test
    void loginRejectsMissingRequiredField() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"auth-test@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void meReturnsCurrentUserForValidJwtWithoutPasswordFields() throws Exception {
        register(TEST_EMAIL, TEST_PASSWORD, "Test Reader")
                .andExpect(status().isCreated());
        String token = login(TEST_EMAIL, TEST_PASSWORD)
                .andReturn().getResponse().getContentAsString();
        String jwt = objectMapper.readTree(token).get("token").asText();

        MvcResult result = mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.displayName").value("Test Reader"))
                .andReturn();

        assertResponseDoesNotExposePassword(result);
    }

    @Test
    void meRejectsMissingTokenAsJson401() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Chưa đăng nhập"));
    }

    @Test
    void meRejectsMalformedTokenAsJson401() throws Exception {
        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token không hợp lệ hoặc đã hết hạn"));
    }

    @Test
    void meRejectsExpiredTokenAsJson401() throws Exception {
        Instant expiredAt = Instant.parse("2020-01-01T00:00:00Z");
        JwtUtil expiredJwtUtil = new JwtUtil(
                jwtSecret, 86_400_000, Clock.fixed(expiredAt, ZoneOffset.UTC));
        String expiredToken = expiredJwtUtil.generateToken(1L, TEST_EMAIL);

        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token không hợp lệ hoặc đã hết hạn"));
    }

    @Test
    void protectedEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/private-resource"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void meRejectsBasicAuthorizationHeaderAsJson401() throws Exception {
        mockMvc.perform(get(ME_URL).header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Chưa đăng nhập"));
    }

    @Test
    void postMeReturnsMethodNotAllowedJsonWhenAuthenticated() throws Exception {
        mockMvc.perform(post(ME_URL).header("Authorization", "Bearer " + createValidToken()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Phương thức HTTP không được hỗ trợ"));
    }

    @Test
    void authenticatedUnknownPathReturnsNotFoundJson() throws Exception {
        mockMvc.perform(get("/api/path-that-does-not-exist")
                        .header("Authorization", "Bearer " + createValidToken()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Không tìm thấy tài nguyên"));
    }

    @Test
    void unsupportedMediaTypeReturns415Json() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Loại nội dung không được hỗ trợ"));
    }

    @Test
    void registrationAndLoginNormalizeEmailCaseAndWhitespace() throws Exception {
        register("  Mixed.Case@Example.com  ", TEST_PASSWORD, "Test Reader")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("mixed.case@example.com"));

        login("  MIXED.CASE@EXAMPLE.COM ", TEST_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mixed.case@example.com"));
    }

    @Test
    void malformedJsonReturnsBadRequestJson() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Dữ liệu yêu cầu không hợp lệ"));
    }

    @Test
    void meRejectsTokenSignedByDifferentSecretAsJson401() throws Exception {
        String otherSecret = "another-test-jwt-secret-key-with-at-least-32-characters";
        String wrongSignatureToken = Jwts.builder()
                .subject(TEST_EMAIL)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(otherSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get(ME_URL).header("Authorization", "Bearer " + wrongSignatureToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token không hợp lệ hoặc đã hết hạn"));
    }

    @Test
    void registrationRejectsPasswordLongerThanBcryptLimit() throws Exception {
        register(TEST_EMAIL, "a".repeat(73), "Test Reader")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void jwtUtilRejectsSecretShorterThanThirtyTwoCharacters() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new JwtUtil("too-short-secret", 86_400_000, Clock.systemUTC()));
        assertTrue(exception.getMessage().contains("32"));
    }

    @Test
    void emailUniqueConstraintViolationReturnsConflict() {
        SQLException sqlException = new SQLException(
                "duplicate key value violates unique constraint users_email_key", "23505");
        var response = apiExceptionHandler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate email", sqlException));

        assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT));
        assertEquals("Email đã tồn tại", response.getBody().get("error"));
    }

    @Test
    void nonEmailConstraintViolationDoesNotClaimEmailConflict() {
        SQLException sqlException = new SQLException(
                "duplicate key value violates unique constraint users_display_name_key", "23505");
        var response = apiExceptionHandler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate display name", sqlException));

        assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST));
        assertEquals("Dữ liệu vi phạm ràng buộc", response.getBody().get("error"));
    }

    @Test
    void responseStatusExceptionPreservesStatusAndReturnsJsonError() {
        var response = apiExceptionHandler.handleException(
                new ResponseStatusException(HttpStatus.GONE, "Tài nguyên đã bị xóa"));

        assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.GONE));
        assertEquals("Yêu cầu không thể xử lý", response.getBody().get("error"));
    }

    @Test
    void unexpectedExceptionReturnsGenericInternalServerError() {
        var response = apiExceptionHandler.handleException(
                new IllegalStateException("internal implementation detail"));

        assertTrue(response.getStatusCode().isSameCodeAs(HttpStatus.INTERNAL_SERVER_ERROR));
        assertEquals("Đã xảy ra lỗi hệ thống", response.getBody().get("error"));
    }

    private org.springframework.test.web.servlet.ResultActions register(
            String email, String password, String displayName) throws Exception {
        return mockMvc.perform(post(REGISTER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterPayload(email, password, displayName))));
    }

    private org.springframework.test.web.servlet.ResultActions login(
            String email, String password) throws Exception {
        return mockMvc.perform(post(LOGIN_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginPayload(email, password))));
    }

    private String createValidToken() {
        return Jwts.builder()
                .subject(TEST_EMAIL)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private void assertResponseDoesNotExposePassword(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertFalse(response.has("password"));
        assertFalse(response.has("passwordHash"));
        assertFalse(response.has("password_hash"));
    }

    private record RegisterPayload(String email, String password, String displayName) {}

    private record LoginPayload(String email, String password) {}
}
