# Báo cáo hoàn thiện Backend Auth - Vòng 3

**Ngày:** 27/10/2026
**Nhánh:** `feature/initial-upload`
**Phạm vi:** Backend `truyenapi`, package `com.nhom7.truyenapi`.
**Trạng thái kiểm thử:** 30/30 test đạt; xem log đầy đủ tại [test-results.txt](./test-results.txt).

## 1. Xác minh handler lỗi framework

Trước vòng 3, `ApiExceptionHandler` bắt `Exception` chung và trả `500` cho mọi exception chưa có handler riêng. Spring 7 MVC exceptions như `HttpRequestMethodNotSupportedException`, `NoResourceFoundException` và `NoHandlerFoundException` implement `ErrorResponse` nhưng không nhất thiết kế thừa cùng một exception base; riêng `ResponseStatusException` là `ErrorResponseException`. Vì vậy chúng có thể rơi vào handler `Exception` và bị đổi status thành 500.

Đã cập nhật handler tổng quát:

- Nếu exception implement `ErrorResponse`, response giữ `getStatusCode()` ban đầu, nhưng body được chuẩn hóa thành `{"error":"..."}` với thông báo chung phù hợp status.
- Các status được ánh xạ rõ: 400, 401, 403, 404, 405 và 415.
- `ResponseStatusException` cũng giữ status mà không gửi reason/detail tùy ý về client.
- Chỉ exception không implement `ErrorResponse` mới log stack trace ở server và trả `500 {"error":"Đã xảy ra lỗi hệ thống"}`.

Kiểm chứng qua request tích hợp: POST `/auth/me` đã xác thực trả 405 JSON; GET đường dẫn không tồn tại đã xác thực trả 404 JSON; media type không hỗ trợ trả 415 JSON. Test trực tiếp `ResponseStatusException` cũng xác minh status được giữ.

## 2. DataIntegrityViolationException

Trước vòng này, mọi `DataIntegrityViolationException` đều trả `409 Email đã tồn tại`, kể cả vi phạm constraint khác. Hiện handler chỉ dùng 409 cho SQLState `23505` khi chẩn đoán SQL xác định constraint/cột email (ví dụ `Key (email)` hoặc `users_email_key`). Integrity violation khác trả 400 `Dữ liệu vi phạm ràng buộc` và được log phía server.

**Giới hạn cần lưu ý:** cách nhận diện phụ thuộc SQLState `23505` và thông báo constraint từ driver có nêu tên cột hoặc constraint email. Với driver/database có định dạng khác hoặc chỉ trả tên constraint tổng quát không chứa `email`, handler cố ý không khẳng định email bị trùng; trường hợp đó nhận phản hồi 400 chung. Test hiện có kiểm tra thông báo kiểu PostgreSQL `users_email_key` và unique constraint khác email.

## 3. Ca mới vòng 3

- POST `/auth/me` với token hợp lệ → 405 JSON.
- GET đường dẫn không tồn tại với token hợp lệ → 404 JSON, không phải 500.
- GET `/api/private-resource` chưa đăng nhập → 401 JSON, xác nhận đường dẫn ngoài danh sách công khai.
- Đăng ký cùng email khác hoa/thường/khoảng trắng → 409.
- Header `Authorization: Basic ...` → 401 JSON.
- Thêm kiểm thử 415, phân loại SQL constraint email/constraint khác, giữ status `ResponseStatusException`, và bảo đảm lỗi bất ngờ trả 500 không lộ chi tiết.

Toàn bộ test method và ID được ánh xạ tại [test-cases-auth.md](./test-cases-auth.md).

## 4. Hướng dẫn cấu hình `JWT_SECRET`

Đã thêm `.env.example` ở root, chỉ có tên biến với giá trị rỗng; `.env` được Git ignore. Ứng dụng không tự động nạp `.env`.

Chạy backend trong Windows PowerShell:

```powershell
$env:JWT_SECRET = "<your-random-secret-at-least-32-characters>"
cd .\truyenapi
.\mvnw.cmd spring-boot:run
```

Thay placeholder bằng secret ngẫu nhiên riêng dài tối thiểu 32 ký tự. Không ghi secret thật vào repo hoặc báo cáo. Cấu hình chỉ tồn tại trong phiên PowerShell hiện tại; ứng dụng fail-fast nếu thiếu hoặc khóa quá ngắn. Xem thêm mục **Chạy backend** trong `README.md`.

## 5. Kết quả test và số lượng

Lệnh `.\\mvnw.cmd test` chạy trong `truyenapi`, cấu hình H2 in-memory:

- `AuthIntegrationTests`: **29** test.
- `TruyenapiApplicationTests`: **1** test.
- Tổng: **30 test, 0 failure, 0 error, 0 skipped**.
- Thời gian thực thi: **32.614 giây** (bắt đầu 21:00:11, kết thúc 21:00:44, UTC+07).
- Commit tại thời điểm chạy: `187c1b8899cec9483e2338378cc70c161765c88d`.

File [test-results.txt](./test-results.txt) lưu toàn bộ log Maven, thời điểm bắt đầu/kết thúc, thời lượng, mã exit và mã commit tại thời điểm chạy. Báo cáo ca và mapping method đầy đủ ở [test-cases-auth.md](./test-cases-auth.md).

## 6. File dự kiến trong commit

**Nhóm 1 - Code và test:** `ApiExceptionHandler.java`, `AuthIntegrationTests.java` cùng các file Auth/H2 đã thay đổi từ vòng trước (controller/service/DTO/security/JWT/configuration/dependencies/test setup).

**Nhóm 2 - Hướng dẫn và hồ sơ:** `.env.example`, `.gitignore`, `README.md`, báo cáo Auth và tài liệu round3.

Đã tạo commit code/test riêng và sẽ commit tài liệu/cấu hình môi trường thành nhóm thứ hai. Mã commit và trạng thái push được báo trong phần bàn giao.

## 7. Chưa thực hiện trong phạm vi

- Chưa có chức năng quên/đặt lại mật khẩu, xác minh email, giới hạn số lần đăng nhập hay thu hồi JWT.
- Chưa kiểm thử PostgreSQL thực tế; integration tests dùng H2.
