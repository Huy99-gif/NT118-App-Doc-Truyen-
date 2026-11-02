package com.nhom7.truyenapi.config;

import com.nhom7.truyenapi.entity.PasswordResetToken;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.repository.PasswordResetTokenRepository;
import com.nhom7.truyenapi.repository.UserRepository;
import com.nhom7.truyenapi.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordResetIntegrationTests {
    private static final String EMAIL = "password-reset@example.com";
    private static final String OLD_PASSWORD = "MatKhauCu123";
    private static final String NEW_PASSWORD = "MatKhauMoi123";
    private static final String FORGOT_URL = "/auth/forgot-password";
    private static final String RESET_URL = "/auth/reset-password";
    private static final String FORGOT_MESSAGE =
            "Nếu email tồn tại, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu.";
    private static final String INVALID_TOKEN_MESSAGE =
            "Token không hợp lệ hoặc đã hết hạn.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordResetTokenRepository resetTokens;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void clearExistingData() {
        resetTokens.deleteAll();
        users.deleteAll();
    }

    @AfterEach
    void removeTestData() {
        resetTokens.deleteAll();
        users.deleteAll();
    }

    @Test
    void forgotPasswordStoresOnlyTokenHashAndSendsRawToken() throws Exception {
        User user = createUser(EMAIL, OLD_PASSWORD);

        MvcResult result = forgot(EMAIL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(FORGOT_MESSAGE))
                .andReturn();

        String rawToken = captureSentToken(user.getEmail(), 0);
        PasswordResetToken savedToken = resetTokens.findAll().get(0);
        assertNotEquals(rawToken, savedToken.getToken());
        assertEquals(hashToken(rawToken), savedToken.getToken());
        assertTrue(savedToken.getExpiresAt().isAfter(Instant.now()));
        assertTrue(savedToken.getExpiresAt().isBefore(Instant.now().plusSeconds(16 * 60L)));

        String response = result.getResponse().getContentAsString();
        assertFalse(response.contains(rawToken));
        assertFalse(response.contains(savedToken.getToken()));
        assertFalse(response.contains("passwordHash"));
        verify(emailService, timeout(2000))
                .sendPasswordResetEmail(eq(user.getEmail()), eq(rawToken), eq(15L));
    }

    @Test
    void forgotPasswordReturnsSameMessageForUnknownEmail() throws Exception {
        forgot("unknown@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(FORGOT_MESSAGE));

        assertTrue(resetTokens.findAll().isEmpty());
        verify(emailService, after(500).never())
                .sendPasswordResetEmail(anyString(), anyString(), anyLong());
    }

    @Test
    void forgotPasswordReturnsSameStatusAndBodyForKnownAndUnknownEmails() throws Exception {
        MvcResult unknownEmailResult = forgot("unknown@example.com").andReturn();
        createUser(EMAIL, OLD_PASSWORD);
        MvcResult knownEmailResult = forgot(EMAIL).andReturn();

        assertEquals(unknownEmailResult.getResponse().getStatus(),
                knownEmailResult.getResponse().getStatus());
        assertEquals(unknownEmailResult.getResponse().getContentAsString(),
                knownEmailResult.getResponse().getContentAsString());
        verify(emailService, timeout(2000))
                .sendPasswordResetEmail(eq(EMAIL), anyString(), eq(15L));
    }

    @Test
    void forgotPasswordRejectsBlankOrMalformedEmail() throws Exception {
        forgot(" ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
        forgot("not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyLong());
    }

    @Test
    void forgotPasswordInvalidatesPreviousUnusedTokens() throws Exception {
        createUser(EMAIL, OLD_PASSWORD);
        forgot(EMAIL).andExpect(status().isOk());
        forgot(EMAIL).andExpect(status().isOk());

        List<PasswordResetToken> savedTokens = resetTokens.findAll();
        assertEquals(2, savedTokens.size());
        assertTrue(savedTokens.get(0).isUsed());
        assertFalse(savedTokens.get(1).isUsed());

        String firstRawToken = captureSentToken(EMAIL, 0);
        reset(firstRawToken, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(INVALID_TOKEN_MESSAGE));
    }

    @Test
    void resetPasswordChangesPasswordAndInvalidatesTokens() throws Exception {
        createUser(EMAIL, OLD_PASSWORD);
        forgot(EMAIL).andExpect(status().isOk());
        String rawToken = captureSentToken(EMAIL, 0);
        PasswordResetToken otherToken = new PasswordResetToken();
        otherToken.setUser(users.findByEmail(EMAIL).orElseThrow());
        otherToken.setToken(hashToken("another-active-token"));
        otherToken.setExpiresAt(Instant.now().plusSeconds(15 * 60L));
        resetTokens.save(otherToken);

        MvcResult result = reset(rawToken, NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công."))
                .andReturn();

        assertFalse(result.getResponse().getContentAsString().contains("passwordHash"));
        assertFalse(result.getResponse().getContentAsString().contains(hashToken(rawToken)));
        assertTrue(resetTokens.findAll().stream().allMatch(PasswordResetToken::isUsed));
        reset("another-active-token", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(INVALID_TOKEN_MESSAGE));
        login(EMAIL, OLD_PASSWORD).andExpect(status().isUnauthorized());
        login(EMAIL, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void resetPasswordTrimsTokenBeforeHashing() throws Exception {
        createUser(EMAIL, OLD_PASSWORD);
        forgot(EMAIL).andExpect(status().isOk());
        String rawToken = captureSentToken(EMAIL, 0);

        reset("  " + rawToken + "  ", NEW_PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công."));
    }

    @Test
    void resetPasswordRejectsUnknownToken() throws Exception {
        reset("not-a-valid-token", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(INVALID_TOKEN_MESSAGE));
    }

    @Test
    void resetPasswordRejectsAlreadyUsedToken() throws Exception {
        createUser(EMAIL, OLD_PASSWORD);
        forgot(EMAIL).andExpect(status().isOk());
        String rawToken = captureSentToken(EMAIL, 0);

        reset(rawToken, NEW_PASSWORD).andExpect(status().isOk());
        reset(rawToken, "AnotherPassword123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(INVALID_TOKEN_MESSAGE));
    }

    @Test
    void resetPasswordRejectsExpiredToken() throws Exception {
        User user = createUser(EMAIL, OLD_PASSWORD);
        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setUser(user);
        expiredToken.setToken(hashToken("expired-token"));
        expiredToken.setExpiresAt(Instant.now().minusSeconds(60));
        resetTokens.save(expiredToken);

        reset("expired-token", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(INVALID_TOKEN_MESSAGE));
    }

    @Test
    void resetPasswordRejectsPasswordsOutsideBcryptLengthRules() throws Exception {
        MvcResult tooShortResult = reset("any-token", "12345")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Mật khẩu phải có ít nhất 6 ký tự"))
                .andReturn();
        MvcResult tooLongResult = reset("any-token", "a".repeat(73))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Mật khẩu không được vượt quá 72 ký tự"))
                .andReturn();

        assertEquals("{\"error\":\"Mật khẩu phải có ít nhất 6 ký tự\"}",
                tooShortResult.getResponse().getContentAsString());
        assertEquals("{\"error\":\"Mật khẩu không được vượt quá 72 ký tự\"}",
                tooLongResult.getResponse().getContentAsString());
    }

    @Test
    void markUsedIfValidOnlyMarksAnUnusedUnexpiredTokenOnce() throws Exception {
        User user = createUser(EMAIL, OLD_PASSWORD);
        PasswordResetToken validToken = new PasswordResetToken();
        validToken.setUser(user);
        validToken.setToken(hashToken("valid-token"));
        validToken.setExpiresAt(Instant.now().plusSeconds(60));
        validToken = resetTokens.save(validToken);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Long validTokenId = validToken.getId();
        Integer firstUpdate = transactionTemplate.execute(
                status -> resetTokens.markUsedIfValid(validTokenId, Instant.now()));
        Integer secondUpdate = transactionTemplate.execute(
                status -> resetTokens.markUsedIfValid(validTokenId, Instant.now()));
        assertEquals(Integer.valueOf(1), firstUpdate);
        assertEquals(Integer.valueOf(0), secondUpdate);

        PasswordResetToken expiredToken = new PasswordResetToken();
        expiredToken.setUser(user);
        expiredToken.setToken(hashToken("expired-atomic-token"));
        expiredToken.setExpiresAt(Instant.now().minusSeconds(1));
        Long expiredTokenId = resetTokens.save(expiredToken).getId();
        Integer expiredUpdate = transactionTemplate.execute(
                status -> resetTokens.markUsedIfValid(expiredTokenId, Instant.now()));
        assertEquals(Integer.valueOf(0), expiredUpdate);
    }

    @Test
    void forgotPasswordHidesEmailDeliveryFailure() throws Exception {
        createUser(EMAIL, OLD_PASSWORD);
        doThrow(new IllegalStateException("mail delivery failed"))
                .when(emailService).sendPasswordResetEmail(anyString(), anyString(), anyLong());

        forgot(EMAIL)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(FORGOT_MESSAGE));
        verify(emailService, timeout(2000))
                .sendPasswordResetEmail(eq(EMAIL), anyString(), eq(15L));
    }

    private User createUser(String email, String password) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDisplayName("Password Reset Reader");
        return users.save(user);
    }

    private org.springframework.test.web.servlet.ResultActions forgot(String email) throws Exception {
        return mockMvc.perform(post(FORGOT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions reset(String token, String password)
            throws Exception {
        return mockMvc.perform(post(RESET_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + token + "\",\"newPassword\":\"" + password + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password)
            throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private String captureSentToken(String recipient, int callIndex) throws Exception {
        var recipientCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var tokenCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        var expiryCaptor = org.mockito.ArgumentCaptor.forClass(Long.class);
        verify(emailService, timeout(2000).atLeast(callIndex + 1))
                .sendPasswordResetEmail(
                        recipientCaptor.capture(), tokenCaptor.capture(), expiryCaptor.capture());
        assertEquals(recipient, recipientCaptor.getAllValues().get(callIndex));
        assertEquals(15L, expiryCaptor.getAllValues().get(callIndex));
        return tokenCaptor.getAllValues().get(callIndex);
    }

    private String hashToken(String token) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
