---
applyTo: "truyenapi/**/*.java"
---
# Quy ước code Java cho backend truyenapi

- Package gốc `com.nhom7.truyenapi`; dòng `package` phải khớp thư mục chứa file.
- Tiêm dependency bằng **constructor** (không dùng `@Autowired` trên field).
- Controller dùng `@RestController`, `@RequestMapping`, trả `ResponseEntity`. Request body gắn `@Valid @RequestBody`.
- Hàm trong `repository` đặt theo quy tắc Spring Data: `existsByEmail`, `findByEmail`, `findByStoryId(Long storyId, Pageable pageable)`.
- Chuỗi thông báo lỗi trả về cho người dùng viết bằng tiếng Việt.

Mẫu DTO (record kèm validation):
```java
public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
```

Mẫu trả lỗi thống nhất:
```java
return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Email đã tồn tại"));
```

Mẫu entity: `@Entity @Table(name = "stories")`, cột `@Column(name = "cover_url")`, có getter/setter viết tay.

Khi thêm endpoint mới: cập nhật danh sách đường dẫn công khai trong `SecurityConfig` **chỉ** nếu endpoint đó không cần đăng nhập.
