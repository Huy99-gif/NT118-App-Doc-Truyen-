package com.nhom7.truyenapi.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final long expirationMs;
    private final Clock clock;

    @Autowired
    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this(secret, expirationMs, Clock.systemUTC());
    }

    JwtUtil(String secret, long expirationMs, Clock clock) {
        // khóa bí mật phải đủ 256 bits ko là jjwt nó throw lỗi
        if (secret == null || secret.isBlank() || secret.length() < 32
                || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret phải có ít nhất 32 ký tự");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.clock = clock;
    }

    public String generateToken(Long userId, String email) {
        Date now = Date.from(clock.instant());
        return Jwts.builder()
                .subject(email)
                .claim("uid", userId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        // parse token nếu hết hạn hoặc sai chữ ký nó tự ném exception
        String email = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("JWT subject is missing");
        }
        return email;
    }
}