package com.nhom7.truyenapi.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final Pattern EMAIL_UNIQUE_CONSTRAINT = Pattern.compile(
            "(?i)(users\\s*\\(\\s*email\\b|key\\s*\\(\\s*email\\s*\\)|"
                    + "users_email_key|unique.*\\bemail\\b|\\bemail\\b.*unique)");

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .filter(defaultMessage -> defaultMessage != null && !defaultMessage.isBlank())
                .orElse("Dữ liệu không hợp lệ");
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {
        if (isEmailUniqueViolation(exception)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email đã tồn tại"));
        }

        logger.warn("Vi phạm ràng buộc dữ liệu", exception);
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Dữ liệu vi phạm ràng buộc"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableMessage(
            HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "Dữ liệu yêu cầu không hợp lệ"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception exception) {
        if (exception instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return ResponseEntity.status(status)
                    .body(Map.of("error", errorMessage(status)));
        }

        logger.error("Lỗi không mong đợi khi xử lý request", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Đã xảy ra lỗi hệ thống"));
    }

    private boolean isEmailUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())
                    && sqlException.getMessage() != null
                    && EMAIL_UNIQUE_CONSTRAINT.matcher(sqlException.getMessage()).find()) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String errorMessage(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "Yêu cầu không hợp lệ";
            case 404 -> "Không tìm thấy tài nguyên";
            case 405 -> "Phương thức HTTP không được hỗ trợ";
            case 415 -> "Loại nội dung không được hỗ trợ";
            case 401 -> "Chưa đăng nhập";
            case 403 -> "Bạn không có quyền truy cập";
            default -> "Yêu cầu không thể xử lý";
        };
    }
}
