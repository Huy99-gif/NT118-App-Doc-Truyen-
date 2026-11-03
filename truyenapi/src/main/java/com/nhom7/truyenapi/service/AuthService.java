package com.nhom7.truyenapi.service;

import com.nhom7.truyenapi.config.JwtUtil;
import com.nhom7.truyenapi.dto.LoginRequest;
import com.nhom7.truyenapi.dto.LoginResponse;
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

import java.util.UUID;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS = "Sai email hoặc mật khẩu";

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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
