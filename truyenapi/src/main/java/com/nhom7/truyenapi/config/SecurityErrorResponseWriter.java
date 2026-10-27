package com.nhom7.truyenapi.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SecurityErrorResponseWriter {
    private static final String AUTHENTICATION_REQUIRED = "Chưa đăng nhập";
    private static final String FORBIDDEN = "Bạn không có quyền truy cập";
    private static final String INVALID_TOKEN = "Token không hợp lệ hoặc đã hết hạn";

    public void writeUnauthorized(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
    }

    public void writeUnauthorizedToken(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, INVALID_TOKEN);
    }

    public void writeForbidden(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.FORBIDDEN, FORBIDDEN);
    }

    private void write(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
