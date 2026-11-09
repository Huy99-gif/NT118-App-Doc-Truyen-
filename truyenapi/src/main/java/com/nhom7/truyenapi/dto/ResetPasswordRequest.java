package com.nhom7.truyenapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Token không được để trống")
        @Size(max = 255, message = "Token không hợp lệ")
        String token,
        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
        @Size(max = 72, message = "Mật khẩu không được vượt quá 72 ký tự")
        String newPassword) {
}
