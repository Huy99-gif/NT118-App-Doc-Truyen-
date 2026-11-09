package com.nhom7.truyenapi.service;

import com.nhom7.truyenapi.config.JwtUtil;
import com.nhom7.truyenapi.dto.LoginRequest;
import com.nhom7.truyenapi.dto.LoginResponse;
import com.nhom7.truyenapi.dto.ForgotPasswordRequest;
import com.nhom7.truyenapi.dto.MessageResponse;
import com.nhom7.truyenapi.dto.RegisterRequest;
import com.nhom7.truyenapi.dto.RegisterResponse;
import com.nhom7.truyenapi.dto.ResetPasswordRequest;
import com.nhom7.truyenapi.dto.UserResponse;
import com.nhom7.truyenapi.entity.PasswordResetToken;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.event.PasswordResetRequestedEvent;
import com.nhom7.truyenapi.exception.ApiException;
import com.nhom7.truyenapi.repository.PasswordResetTokenRepository;
import com.nhom7.truyenapi.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import com.nhom7.truyenapi.dto.RegisterRequest;
import com.nhom7.truyenapi.dto.RegisterResponse;
import com.nhom7.truyenapi.dto.UserResponse;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.exception.ApiException;
import com.nhom7.truyenapi.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import java.util.UUID;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS = "Sai email hoặc mật khẩu";
    private static final String FORGOT_PASSWORD_MESSAGE =
            "Nếu email tồn tại, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu.";
    private static final String INVALID_RESET_TOKEN =
            "Token không hợp lệ hoặc đã hết hạn.";
    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;
    private final long resetTokenExpiryMinutes;
    private final String dummyPasswordHash;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository users,
                       PasswordResetTokenRepository resetTokens,
                       PasswordEncoder encoder,
                       JwtUtil jwtUtil,
                       ApplicationEventPublisher eventPublisher,
                       @Value("${app.reset-token.expiry-minutes:15}") long resetTokenExpiryMinutes) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.eventPublisher = eventPublisher;
        this.resetTokenExpiryMinutes = resetTokenExpiryMinutes;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;
    private final String dummyPasswordHash;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.users = users;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
        this.dummyPasswordHash = encoder.encode(UUID.randomUUID().toString());
    }

    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        User savedUser = users.save(user);
        return new RegisterResponse(savedUser.getId(), savedUser.getEmail());
    }

    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            encoder.matches(request.password(), dummyPasswordHash);
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }
        if (!encoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        return new LoginResponse(token, user.getEmail(), user.getDisplayName());
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }

        User user = users.findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            return new MessageResponse(FORGOT_PASSWORD_MESSAGE);
        }

        resetTokens.findAllByUserAndUsedFalse(user).forEach(token -> token.setUsed(true));

        String rawToken = generateResetToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(hashToken(rawToken));
        resetToken.setExpiresAt(Instant.now().plus(resetTokenExpiryMinutes, ChronoUnit.MINUTES));
        resetTokens.save(resetToken);

        eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                user.getEmail(), rawToken, resetTokenExpiryMinutes));
        return new MessageResponse(FORGOT_PASSWORD_MESSAGE);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokens.findByToken(hashToken(request.token().trim()))
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, INVALID_RESET_TOKEN));
        Long userId = resetToken.getUser().getId();

        if (resetTokens.markUsedIfValid(resetToken.getId(), Instant.now()) == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, INVALID_RESET_TOKEN);
        }

        User user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, INVALID_RESET_TOKEN));
        user.setPasswordHash(encoder.encode(request.newPassword()));
        users.save(user);

        resetTokens.findAllByUserAndUsedFalse(user).forEach(token -> token.setUsed(true));
        return new MessageResponse("Đặt lại mật khẩu thành công.");
    }

    private String generateResetToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Không thể khởi tạo SHA-256", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
