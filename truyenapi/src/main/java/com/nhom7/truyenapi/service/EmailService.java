package com.nhom7.truyenapi.service;

public interface EmailService {
    void sendPasswordResetEmail(String recipient, String token, long expiryMinutes);
}
