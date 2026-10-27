package com.nhom7.truyenapi.controller;

import com.nhom7.truyenapi.config.JwtUtil;
import com.nhom7.truyenapi.dto.LoginRequest;
import com.nhom7.truyenapi.dto.RegisterRequest;
import com.nhom7.truyenapi.entity.User;
import com.nhom7.truyenapi.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtUtil jwtUtil) {
        this.users = users;
        this.encoder = encoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email đã tồn tại"));
        }
        User u = new User();
        u.setEmail(req.email());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setDisplayName(req.displayName());
        users.save(u);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("id", u.getId(), "email", u.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        User u = users.findByEmail(req.email()).orElse(null);
        if (u == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Sai email hoặc mật khẩu"));
        }
        String token = jwtUtil.generateToken(u.getId(), u.getEmail());
        return ResponseEntity.ok(Map.of(
                "token", token,
                "email", u.getEmail(),
                "displayName", u.getDisplayName()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        User u = users.findByEmail(auth.getName()).orElse(null);
        if (u == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(Map.of(
                "id", u.getId(),
                "email", u.getEmail(),
                "displayName", u.getDisplayName()));
    }
}