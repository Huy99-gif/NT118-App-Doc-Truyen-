package com.nhom7.truyenapi.event;

public record PasswordResetRequestedEvent(String email, String rawToken, long expiryMinutes) {
}
