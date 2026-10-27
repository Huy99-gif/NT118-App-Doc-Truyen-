# Hướng dẫn dự án cho Copilot (đọc kỹ trước khi viết code)

## 1. Dự án là gì
Đồ án môn học: **ứng dụng đọc truyện online** cho Android, nhóm 3 người (TV1, TV2, TV3). Backend tự viết, app Android viết bằng Java.
Trả lời bằng **tiếng Việt**. Tên class, biến, hàm, bảng, cột viết bằng **tiếng Anh**. Comment ngắn gọn bằng tiếng Việt, chỉ ở chỗ khó hiểu. Người dùng là sinh viên cần hiểu code để bảo vệ, nên khi viết code hãy giải thích ngắn gọn vì sao viết như vậy.

## 2. Cấu trúc repo (một repo duy nhất)
```
truyen-project/
├─ android-app/   app Android (Java, MVVM) - chưa làm
├─ truyenapi/     backend Spring Boot (Maven)
├─ deploy/        docker-compose.yml (PostgreSQL), sau này file Kubernetes
└─ docs/          tài liệu, ERD, API, báo cáo
```
Backend package gốc: `com.nhom7.truyenapi`, gồm các thư mục `config`, `controller`, `dto`, `entity`, `repository` (sẽ thêm `service`, `exception`).

## 3. Công nghệ đã chốt (KHÔNG tự ý đổi)
- Java **17** (không nâng lên bản khác, không sửa `<java.version>` trong `pom.xml`).
- Spring Boot **4.1.2** (Spring Framework 7, Spring Security 7, Hibernate 7, Jakarta EE). Build bằng Maven Wrapper: `.\mvnw.cmd spring-boot:run`.
- PostgreSQL **16** chạy bằng Docker (`deploy/docker-compose.yml`): container `truyen-db`, database `truyendb`, user `truyen`, cổng 5432.
- Cấu hình ở `truyenapi/src/main/resources/application.properties` (**không** tạo thêm `application.yml`).
- JWT dùng thư viện JJWT 0.12.x. Khóa bí mật (`app.jwt.secret`) phải dài **tối thiểu 32 ký tự**, đọc từ biến môi trường `JWT_SECRET` có giá trị mặc định.
- Không dùng Lombok (tự viết getter/setter hoặc dùng `record` cho DTO).
- Không thêm thư viện mới vào `pom.xml` mà chưa hỏi; nếu cần, nêu rõ tên thư viện và lý do.

**Lưu ý Spring Boot 4 là bản mới:** nhiều ví dụ trên mạng viết cho Spring Boot 3. Trước khi dùng một class hoặc import, hãy kiểm tra nó tồn tại trong các thư viện trong `pom.xml`. Nếu không chắc một API còn dùng được ở Spring Boot 4 / Spring Security 7, hãy nói rõ là chưa chắc thay vì đoán. Dùng cú pháp lambda DSL cho `HttpSecurity`. Jackson có thể dùng gói mới `tools.jackson` ở Spring Boot 4, kiểm tra import trước khi viết.

## 4. Múi giờ (đã từng gây lỗi)
`TruyenapiApplication` có khối `static { TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")); }` để tránh lỗi `Asia/Saigon` với PostgreSQL. **Không xóa** khối này. Cấu hình `spring.jpa.properties.hibernate.jdbc.time_zone=UTC` giữ nguyên: thời gian lưu trong database theo UTC.

## 5. Trạng thái hiện tại
- Xong: `POST /auth/register` (201 thành công, 409 trùng email, 400 dữ liệu sai).
- Đang làm: `POST /auth/login` trả JWT, `GET /auth/me`, `JwtUtil`, `JwtFilter`, `SecurityConfig`.
- Chưa làm: quên mật khẩu, truyện/chương, tìm kiếm, tủ sách, lịch sử, bình luận, đánh giá, offline, toàn bộ app Android.

## 6. Quy tắc viết code backend
1. **Phân tầng:** `controller` chỉ nhận request và trả response; logic nghiệp vụ đặt ở `service`; truy cập database ở `repository`. Không đặt logic phức tạp trong controller.
2. **DTO:** request/response dùng `record` đặt trong `dto`, kèm validation (`@NotBlank`, `@Email`, `@Size`...). **Không bao giờ trả entity trực tiếp** ra ngoài, và không bao giờ trả `passwordHash`.
3. **Entity:** mỗi class một bảng, tên bảng số nhiều, snake_case (`users`, `stories`, `chapters`). Cột snake_case bằng `@Column(name = "...")`. Khóa chính `Long id` với `GenerationType.IDENTITY`.
4. **Mật khẩu:** luôn mã hóa bằng `BCryptPasswordEncoder` trước khi lưu.
5. **Bảo mật:** stateless, JWT trong header `Authorization: Bearer <token>`. Đường dẫn công khai: `/auth/register`, `/auth/login`, `/auth/forgot-password`, `/auth/reset-password`, `/error`, `/swagger-ui/**`, `/v3/api-docs/**`. Mọi đường dẫn khác cần đăng nhập. Lấy user hiện tại từ `SecurityContext`, không tin `userId` do client gửi lên.
6. **Lỗi trả về dạng JSON** `{"error": "thông báo tiếng Việt"}` kèm mã HTTP đúng: 400 dữ liệu sai, 401 chưa đăng nhập hoặc sai mật khẩu, 403 không có quyền, 404 không tìm thấy, 409 trùng dữ liệu. Dùng `@RestControllerAdvice` cho lỗi chung.
7. **Phân trang:** danh sách dài (truyện, chương, bình luận) dùng `Pageable` với tham số `page`, `size`; trả về nội dung kèm tổng số trang.
8. **Không viết cứng bí mật** (mật khẩu DB, khóa JWT, SMTP) trong code. Đọc từ biến môi trường với giá trị mặc định cho môi trường phát triển: `${TEN_BIEN:gia_tri_mac_dinh}`.
9. **Một thay đổi, một mục đích:** làm từng tính năng nhỏ, không sửa file không liên quan.

## 7. Cơ sở dữ liệu (PostgreSQL, 12 bảng)
`users`(id, email unique, password_hash, display_name) · `password_reset_tokens`(id, user_id, token unique, expires_at, used) · `authors`(id, name) · `categories`(id, name unique) · `stories`(id, author_id, title, cover_url, summary, status, updated_at) · `story_categories`(story_id, category_id) · `chapters`(id, story_id, chapter_no, title, content, updated_at; updated_at dùng làm version cho offline) · `library`(user_id, story_id, added_at) · `reading_history`(user_id, story_id, chapter_id, char_offset, updated_at; mỗi user một dòng cho mỗi truyện) · `bookmarks`(id, user_id, chapter_id, char_offset, note) · `comments`(id, user_id, story_id, content, created_at) · `ratings`(id, user_id, story_id, stars 1-5, unique(user_id, story_id)).
Hiện dùng `spring.jpa.hibernate.ddl-auto=update` khi phát triển.

## 8. Danh sách API dự kiến
| Nhóm | Endpoint |
|---|---|
| Xác thực | `POST /auth/register`, `/auth/login`, `/auth/forgot-password`, `/auth/reset-password`, `GET /auth/me` |
| Truyện | `GET /stories?type=featured\|latest&category=`, `GET /stories/{id}`, `GET /stories/search?q=&author=&status=` |
| Chương | `GET /stories/{id}/chapters` (phân trang), `GET /chapters/{id}`, `GET /chapters/batch?ids=` (tải offline) |
| Tủ sách | `GET/POST/DELETE /library` |
| Lịch sử, bookmark | `PUT /history`, `GET /history`, `POST/GET/DELETE /bookmarks` |
| Tương tác | `GET/POST /stories/{id}/comments`, `PUT /stories/{id}/rating` |
| Đồng bộ | `POST /sync` (đẩy hàng loạt vị trí đọc và lịch sử khi có mạng lại, giữ bản mới nhất theo `updated_at`) |

## 9. App Android (làm sau, ghi sẵn để thống nhất)
Java, kiến trúc MVVM + Repository; Retrofit + OkHttp + Gson (gắn JWT bằng interceptor); Room (offline, lịch sử); WorkManager (tải chương nền); Paging 3; Glide; SharedPreferences cho cài đặt đọc (cỡ chữ, font, Dark/Sepia); EncryptedSharedPreferences cho token. Không dùng Kotlin, không dùng Jetpack Compose. minSdk 26.
Vị trí đọc lưu theo **offset ký tự/đoạn**, không lưu pixel. Đọc offline theo hướng offline-first: kiểm tra Room trước, không có thì gọi API rồi lưu Room.

## 10. Cách làm việc với Copilot
- Làm **từng bước nhỏ**. Trước khi viết, nêu ngắn gọn kế hoạch và danh sách file sẽ tạo hoặc sửa.
- Sau khi viết, nêu **cách kiểm tra** (lệnh chạy, request Postman mẫu, kết quả mong đợi).
- **Không** tự chạy lệnh git, xóa file, nâng cấp Java hoặc Spring Boot, hay sửa `pom.xml` ngoài phạm vi được yêu cầu.
- Khi gặp lỗi, đọc nguyên nhân gốc ở dòng `Caused by` cuối cùng của log trước khi đề xuất sửa.
- Nếu yêu cầu chưa rõ hoặc thiếu thông tin, hỏi lại một câu thay vì đoán.
