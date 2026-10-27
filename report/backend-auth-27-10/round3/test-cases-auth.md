# Backend Auth - Test cases vòng 3

**Lệnh:** `.\mvnw.cmd test`
**Môi trường:** JUnit 5, Spring Boot Test, MockMvc, H2 in-memory.
**Kết quả cuối:** 29 test `AuthIntegrationTests` + 1 `contextLoads`; tổng 30 test, không lỗi. Chi tiết và log đầy đủ nằm ở [test-results.txt](./test-results.txt).

## Ca kiểm thử và ánh xạ tới test method

Mỗi test method được liệt kê một lần. Các method thực thi kèm assertion rằng response không chứa `password`, `passwordHash` hoặc `password_hash` được ghi rõ trong cột phạm vi.

| ID | Test method | Kiểm tra | Kết quả |
|---|---|---|---|
| AUTH-REG-01 | `registerReturnsCreatedAndDoesNotExposePassword` | Đăng ký thành công 201, DTO không lộ password; hash lưu khác password thô. | Đạt |
| AUTH-REG-02 | `registerDuplicateEmailReturnsConflict` | Email đã đăng ký trả 409. | Đạt |
| AUTH-REG-03 | `registerRejectsInvalidEmail` | Email sai định dạng trả 400. | Đạt |
| AUTH-REG-04 | `registerRejectsBlankEmail` | Email trắng trả 400. | Đạt |
| AUTH-REG-05 | `registerRejectsPasswordShorterThanSixCharacters` | Password dưới 6 ký tự trả 400. | Đạt |
| AUTH-REG-06 | `registerRejectsBlankDisplayName` | Tên hiển thị trắng trả 400. | Đạt |
| AUTH-REG-07 | `registerDuplicateEmailWithDifferentCaseReturnsConflict` | Đăng ký lại cùng email khác hoa/thường và có khoảng trắng trả 409. | Đạt |
| AUTH-REG-08 | `registrationRejectsPasswordLongerThanBcryptLimit` | Password 73 ký tự trả 400. | Đạt |
| AUTH-LOG-01 | `loginReturnsJwtAndDoesNotExposePassword` | Đăng nhập đúng trả 200/JWT; response không lộ password/hash. | Đạt |
| AUTH-LOG-02 | `loginRejectsIncorrectPassword` | Sai password trả 401 với thông báo chung. | Đạt |
| AUTH-LOG-03 | `loginRejectsUnknownEmailWithGenericCredentialsError` | Email không tồn tại trả cùng 401; service thực hiện so hash giả. | Đạt |
| AUTH-LOG-04 | `loginRejectsMissingRequiredField` | Thiếu password trả 400. | Đạt |
| AUTH-ME-01 | `meReturnsCurrentUserForValidJwtWithoutPasswordFields` | JWT hợp lệ trả 200; response không lộ password/hash. | Đạt |
| AUTH-ME-02 | `meRejectsMissingTokenAsJson401` | Thiếu token trả 401 JSON. | Đạt |
| AUTH-ME-03 | `meRejectsMalformedTokenAsJson401` | Token sai định dạng trả 401 JSON, không 500. | Đạt |
| AUTH-ME-04 | `meRejectsExpiredTokenAsJson401` | Token hết hạn trả 401 JSON; dùng `Clock` cố định. | Đạt |
| AUTH-ME-05 | `meRejectsTokenSignedByDifferentSecretAsJson401` | Chữ ký sai trả 401 JSON. | Đạt |
| AUTH-SEC-01 | `protectedEndpointRejectsUnauthenticatedRequest` | `GET /api/private-resource` ngoài danh sách công khai khi chưa đăng nhập trả 401 JSON. | Đạt |
| AUTH-SEC-02 | `meRejectsBasicAuthorizationHeaderAsJson401` | Header `Authorization: Basic ...` trả 401 JSON. | Đạt |
| HTTP-405 | `postMeReturnsMethodNotAllowedJsonWhenAuthenticated` | `POST /auth/me` với JWT hợp lệ trả 405 JSON. | Đạt |
| HTTP-404 | `authenticatedUnknownPathReturnsNotFoundJson` | GET đường dẫn không tồn tại với JWT hợp lệ trả 404 JSON. | Đạt |
| HTTP-415 | `unsupportedMediaTypeReturns415Json` | Content-Type không hỗ trợ trả 415 JSON. | Đạt |
| AUTH-EMAIL-NORM | `registrationAndLoginNormalizeEmailCaseAndWhitespace` | Trim/lowercase email trên register và login. | Đạt |
| REQUEST-JSON | `malformedJsonReturnsBadRequestJson` | JSON sai cú pháp trả 400 JSON. | Đạt |
| AUTH-PASSWORD-RESPONSE | `registerReturnsCreatedAndDoesNotExposePassword`; `loginReturnsJwtAndDoesNotExposePassword`; `meReturnsCurrentUserForValidJwtWithoutPasswordFields` | Không response nào có `password`, `passwordHash` hoặc `password_hash`. | Đạt |
| DATA-EMAIL-UNIQUE | `emailUniqueConstraintViolationReturnsConflict` | SQLState unique violation xác định rõ cột email trả 409. | Đạt |
| DATA-OTHER-CONSTRAINT | `nonEmailConstraintViolationDoesNotClaimEmailConflict` | Unique constraint cột khác trả lỗi ràng buộc 400, không nói email trùng. | Đạt |
| AUTH-STATUS-RESPONSE | `responseStatusExceptionPreservesStatusAndReturnsJsonError` | `ResponseStatusException` giữ HTTP status và trả JSON lỗi. | Đạt |
| AUTH-UNEXPECTED-ERROR | `unexpectedExceptionReturnsGenericInternalServerError` | Exception ngoài dự kiến trả 500, không lộ chi tiết. | Đạt |
| AUTH-JWT-SECRET | `jwtUtilRejectsSecretShorterThanThirtyTwoCharacters` | JWT secret ngắn hơn 32 ký tự bị từ chối. | Đạt |
| APP-CONTEXT | `TruyenapiApplicationTests.contextLoads` | Spring context khởi tạo bằng H2, không cần PostgreSQL ngoài. | Đạt |

## Đối chiếu số lượng

- `AuthIntegrationTests`: **29 test method** (gồm 28 phương thức nghiệp vụ/HTTP và 1 kiểm tra 500 bất ngờ).
- `TruyenapiApplicationTests`: **1 test method**.
- Tổng Maven: **30 test**.
- Một test method có thể bao phủ nhiều assertion/ID (ví dụ kiểm tra response register không lộ password nằm trong test đăng ký thành công). Bảng giữ nguyên tổng method duy nhất, không cộng trùng các assertion.
