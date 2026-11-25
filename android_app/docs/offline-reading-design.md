# Thiết kế: Đọc truyện offline

Ngày: 2026-11-25 · Nhánh gốc: `feature/comments` · Phạm vi: chỉ app Android (`android_app`), **không sửa backend** (`truyenapi`).

## 1. Mục đích và lý do

Người đọc muốn đọc truyện khi không có mạng (đi tàu xe, mạng yếu). Họ **chủ động chọn** chương muốn tải; app không tự tải gì. Nếu muốn, họ có thể tải cả bộ truyện bằng một nút. Khi offline, họ mở app, vào thẳng mục "Đã tải" và đọc; vị trí đọc và dấu trang tạo lúc offline không bị mất mà được gửi lên server khi có mạng lại.

## 2. Mục tiêu / Ngoài phạm vi / Điều không được xảy ra

**Mục tiêu**
- Tải từng chương theo lựa chọn của người dùng, và tải cả truyện khi người dùng bấm "Tải cả truyện".
- Đọc chương đã tải khi không có mạng, kể cả khi mở app từ trạng thái tắt hẳn.
- Tab "Đã tải" trong Tủ sách: xem, mở, xoá truyện và chương đã tải.
- Vị trí đọc (reading history) và dấu trang tạo khi offline được lưu cục bộ và đồng bộ lên server khi có mạng.
- Mở app khi đã có token thì vào thẳng màn chính, không bắt đăng nhập lại.

**Ngoài phạm vi (non-goals)**
- Tự động tải trước chương kế tiếp hay tải ngầm.
- Bình luận offline (đọc hay viết). Bình luận luôn cần mạng.
- Xoá dấu trang khi offline (chỉ hỗ trợ *tạo* dấu trang offline).
- Giải quyết xung đột vị trí đọc giữa nhiều thiết bị. Lần đồng bộ mới nhất sẽ ghi đè, giống hành vi hiện tại.
- Lưu ảnh bìa để xem offline. Khi offline, Glide hiển thị ảnh trong cache nếu còn, không có thì hiện placeholder.
- Giới hạn dung lượng hay tự dọn bộ nhớ.

**Điều không được xảy ra (anti-goals)**
- Người dùng offline bị kẹt ở màn đăng nhập.
- Người dùng mất chương đã tải chỉ vì phiên đăng nhập hết hạn.
- Một tài khoản thấy chương đã tải hoặc dấu trang chờ gửi của tài khoản khác trên cùng máy.
- Dấu trang bị gửi trùng lên server.
- Một nút tải bấm nhầm âm thầm tải cả trăm chương. "Tải cả truyện" phải hỏi xác nhận, có hiện số chương.

## 3. Hiện trạng code (liên quan)

- `ReaderActivity` luôn gọi `GET chapters/{id}` để lấy nội dung. Nội dung chương là **văn bản thuần** (`ChapterDetailResponse.content: String`), không có ảnh.
- Danh sách chương được lấy bằng `GET stories/{id}/chapters?page=0&size=50` ở `StoryDetailActivity.java:165` và `ReaderActivity.java:184`, tức chỉ có **trang đầu**. Backend giới hạn `size ≤ 50` (`PageableFactory.MAX_SIZE`).
- Backend đã có `GET chapters/batch?ids=1,2,3`, tối đa **20 id mỗi lần** (`ChapterService.MAX_BATCH_SIZE`), trả về theo đúng thứ tự id. Retrofit đã khai báo sẵn `TruyenApi.getChaptersBatch` nhưng chưa nơi nào dùng.
- Lưu vị trí đọc: `PUT history {storyId, chapterId, charOffset}`. Tạo dấu trang: `POST bookmarks {chapterId, charOffset, note}`; server giới hạn 500 dấu trang (trả 409).
- Phiên đăng nhập: token lưu trong SharedPreferences (`AuthPreferences`). `LoginActivity` là màn LAUNCHER và **luôn hiện form đăng nhập**, không kiểm tra token đã có. Khi gặp 401, `SessionManager.handleUnauthorized` xoá token và đẩy về `LoginActivity`; `App.java` cũng chuyển hướng khi một Activity resume mà không có token.
- `LoginResponse` có `token, email, displayName` (không có id). `GET auth/me` trả `UserResponse {id, email, displayName}`.
- `LibraryFragment` có 3 tab: Tủ sách / Lịch sử đọc / Dấu trang (`LibraryFragment.java:68-70`).
- App viết bằng **Java**, dùng ViewBinding, Retrofit + Gson, Glide, và version catalog `gradle/libs.versions.toml`. Chưa có Room hay WorkManager.

## 4. Các hướng đã cân nhắc

| Hướng | Mô tả | Đánh đổi |
|---|---|---|
| **1. Room + WorkManager** ✅ **đã chọn** | Room lưu chương, vị trí đọc chờ đồng bộ và dấu trang chờ gửi. WorkManager chạy việc tải và đồng bộ với ràng buộc "có mạng". | Thêm 2 thư viện, Room cần `annotationProcessor` (Java). Đổi lại: việc tải cả truyện vẫn chạy khi rời màn hình hoặc app bị tắt, hệ thống tự chạy lại khi có mạng, có sẵn cơ chế thử lại. |
| 2. Room + ExecutorService | Lưu trữ như trên, tải và đồng bộ bằng luồng nền tự viết. | Việc tải dừng khi app bị tắt; phải tự theo dõi trạng thái mạng và tự viết logic thử lại. |
| 3. File JSON + SharedPreferences | Mỗi chương là một file, chỉ mục lưu trong SharedPreferences. | Không thêm thư viện, nhưng các truy vấn (theo user, đếm chương, dung lượng) và hàng đợi phải tự viết, dễ lỗi. |

Người dùng đã chọn **hướng 1** vì cần "Tải cả truyện" chạy ổn định và cần đồng bộ dấu trang khi có mạng lại.

## 5. Thiết kế

### 5.1 Định danh người dùng (để tách dữ liệu theo tài khoản)

- Khoá người dùng (`ownerKey`) là **email** trả về khi đăng nhập. Lưu vào `AuthPreferences` cùng lúc với token (`KEY_EMAIL`).
- **Đăng xuất hoặc 401 chỉ xoá token**, giữ `ownerKey` cũ trong prefs. Khi đăng nhập lại cùng email thì dữ liệu cũ hiện lại; đăng nhập tài khoản khác thì chỉ thấy dữ liệu của tài khoản đó.
- Trường hợp nâng cấp app (đã có token nhưng chưa lưu email): khi có mạng, gọi `GET auth/me` một lần để lấy email và lưu lại. Khi chưa có `ownerKey`, tab "Đã tải" hiện trạng thái trống và nút tải bị vô hiệu, kèm thông báo "Kết nối mạng một lần để bật tải offline". *(Quyết định của Claude.)*
- Mọi bảng Room đều có cột `ownerKey`, và mọi truy vấn đều lọc theo `ownerKey` hiện tại.

### 5.2 Khởi động app và đăng nhập

- `LoginActivity.onCreate`: nếu `AuthPreferences.getToken() != null` và không có cờ session-expired thì `startActivity(MainActivity)` rồi `finish()` ngay, không hiện form. Cờ hết hạn phiên giữ nguyên logic hiện tại.
- Token hết hạn vẫn được xử lý như cũ: request đầu tiên khi có mạng nhận 401 thì `SessionManager` đưa người dùng về màn đăng nhập. Khi offline không có request nào nên người dùng vẫn đọc được.
- Hệ quả đi kèm (được chấp nhận): khi có mạng, người dùng cũng không phải đăng nhập lại mỗi lần mở app.

**Đọc truyện đã tải khi không còn token** (người dùng chọn: cho phép)
- Khi token bị xoá (đăng xuất, hoặc 401 do hết hạn), `ownerKey` của tài khoản gần nhất vẫn còn trong prefs. Nếu `ownerKey` khác null **và** tài khoản đó có ít nhất một chương đã tải, màn đăng nhập hiện thêm nút **"Đọc truyện đã tải"**. Nút này cũng hiện khi màn đăng nhập mở vì hết phiên.
- Bấm nút sẽ mở `OfflineLibraryActivity`. Activity này chứa cùng `DownloadsFragment` với tab "Đã tải" (5.7), chỉ hiện dữ liệu của `ownerKey` đó, không có bottom navigation và không gọi mạng. Trên thanh tiêu đề có nút "Đăng nhập" để quay về `LoginActivity`.
- Từ đó mở `ReaderActivity` với extra `offline_mode=true`. Ở chế độ này: chỉ đọc bản trong máy, không gọi API; nút bình luận bị vô hiệu; vị trí đọc chỉ ghi vào `local_progress` và `pending_progress`; dấu trang ghi thẳng vào `pending_bookmark`.
- `App.java` (callback `onActivityResumed`) **không chuyển hướng về đăng nhập** khi Activity đang resume là `OfflineLibraryActivity`, hoặc là `ReaderActivity` có `offline_mode=true`.
- Dữ liệu pending tạo lúc này chỉ được gửi khi **đúng tài khoản đó** đăng nhập lại (`SyncWorker` chỉ gửi các dòng có `ownerKey` trùng tài khoản đang có token). Nếu một tài khoản khác đăng nhập thì các dòng này vẫn nằm đó, không bị gửi nhầm.
- Rủi ro được chấp nhận: ai cầm máy sau khi tài khoản trước đăng xuất vẫn đọc được truyện đã tải của tài khoản đó (chỉ đọc, không có dữ liệu tài khoản nào khác).

### 5.3 Lưu trữ cục bộ (Room)

Một database `OfflineDatabase` với 5 bảng:

| Bảng | Khoá chính | Cột chính | Mục đích |
|---|---|---|---|
| `downloaded_story` | (`ownerKey`, `storyId`) | `title`, `author`, `coverUrl`, `totalChapters`, `downloadedAt` | Thông tin truyện để tab "Đã tải" hiển thị khi offline |
| `downloaded_chapter` | (`ownerKey`, `chapterId`) | `storyId`, `chapterNo`, `title`, `content`, `updatedAt`, `downloadedAt`, `sizeBytes` | Nội dung chương |
| `local_progress` | (`ownerKey`, `storyId`) | `chapterId`, `charOffset`, `updatedAt` | Vị trí đọc gần nhất trên máy; **không bao giờ bị xoá khi đồng bộ**. Tab "Đã tải" dùng bảng này để mở đúng chỗ đang đọc. *(Quyết định của Claude, sau review.)* |
| `pending_progress` | (`ownerKey`, `storyId`) | `chapterId`, `charOffset`, `updatedAt` | Hàng đợi đồng bộ vị trí đọc; **ghi đè**, mỗi truyện một dòng, xoá khi gửi thành công |
| `pending_bookmark` | `localId` (tự tăng) | `ownerKey`, `storyId`, `chapterId`, `chapterNo`, `chapterTitle`, `storyTitle`, `charOffset`, `note`, `createdAt` | Dấu trang tạo lúc offline; **hàng đợi**, mỗi dấu trang một dòng |

- `sizeBytes` = số byte UTF-8 của `content`, dùng để hiển thị dung lượng.
- Xoá chương cuối cùng của một truyện thì xoá luôn dòng `downloaded_story` tương ứng.
- Phiên bản DB bắt đầu từ 1. Chưa cần migration, nhưng không bật `fallbackToDestructiveMigration` cho các bảng `pending_*` để tránh mất dữ liệu chưa gửi trong các lần nâng cấp sau. *(Quyết định của Claude.)*

### 5.4 Tải chương (WorkManager)

**Tải một chương:** người dùng bấm ⬇ ở dòng chương trong `StoryDetailActivity`.
- Đưa vào hàng đợi một `ChapterDownloadWorker` với unique work name `download-chapter-{ownerKey}-{chapterId}` (`ExistingWorkPolicy.KEEP`) và ràng buộc `NetworkType.CONNECTED`.
- Worker gọi `GET chapters/{id}`, rồi upsert `downloaded_story` (lấy thông tin từ màn chi tiết, truyền qua `inputData`) và `downloaded_chapter`.

**Tải cả truyện:** bấm nút "Tải cả truyện" thì hiện hộp thoại xác nhận "Tải N chương (~ước lượng)?".
- Đưa vào hàng đợi `StoryDownloadWorker` với unique name `download-story-{ownerKey}-{storyId}` (`KEEP`) và ràng buộc mạng.
- Worker lấy **toàn bộ** danh sách chương bằng cách lặp các trang `size=50` cho tới hết `totalElements`.
- Lọc ra các chương **chưa tải**, hoặc đã tải nhưng `updatedAt` trên server khác `updatedAt` đã lưu (tức là cập nhật lại các chương đã đổi).
- Chia thành lô 20 id và gọi `GET chapters/batch` cho từng lô. **Ghi vào Room sau mỗi lô**, để nếu bị gián đoạn thì lần chạy lại chỉ tải phần còn thiếu.
- Báo tiến độ qua `setProgress` (đã tải / tổng). Gặp lỗi mạng hoặc 5xx thì trả `Result.retry()`; gặp 401 thì `Result.failure()`, vì `AuthInterceptor` sẽ tự xử lý đăng xuất.
- Huỷ: khi đang tải, nút "Tải cả truyện" đổi thành "Huỷ tải (x/N)" và gọi `cancelUniqueWork`. Các chương đã ghi thì giữ lại.

**Giao diện ở `StoryDetailActivity`:** mỗi dòng chương có trạng thái ⬇ (chưa tải) / ⏳ (đang tải) / ✓ (đã tải). Bấm ✓ thì hỏi "Xoá chương đã tải?". Trạng thái lấy từ Room (LiveData) kết hợp `WorkInfo`.

**Danh sách chương ở `StoryDetailActivity`:** hiện chỉ tải 50 chương đầu. Thay đổi này **không** mở rộng danh sách hiển thị. Nút "Tải cả truyện" tự lấy đủ danh sách bên trong worker. *(Quyết định của Claude, để giữ phạm vi gọn.)*

### 5.5 Đọc chương (`ReaderActivity`)

- **Ưu tiên bản trong máy:** `loadChapterContent` tra `downloaded_chapter` theo (`ownerKey`, `chapterId`) trên luồng nền. Nếu có thì hiển thị ngay, không gọi mạng; nếu không có thì gọi API như hiện tại.
- Nếu không có bản cục bộ mà cũng không có mạng thì hiện thông báo "Chương này chưa được tải. Cần kết nối mạng để đọc."
- **Danh sách chương để chuyển trước/sau:** khi `loadChapterListIfNeeded` lỗi mạng (hoặc khi mở từ tab "Đã tải"), dùng danh sách các chương đã tải của truyện đó, sắp theo `chapterNo`. Khi offline, nút trước/sau chỉ chuyển giữa các chương đã tải.
- Kiểm tra `authorization == null` trong `loadChapterContent` bây giờ chỉ áp dụng cho nhánh gọi mạng. Đọc bản cục bộ chỉ cần có `ownerKey`.
- Nút bình luận bị vô hiệu khi offline, bấm vào hiện "Cần kết nối mạng để xem bình luận".

### 5.6 Đồng bộ vị trí đọc và dấu trang

**Vị trí đọc:** `saveReadingProgress` luôn upsert vào `local_progress` và `pending_progress` trước, rồi mới thử gọi `PUT history` như hiện tại.
- Thành công: xoá dòng đó, nhưng chỉ khi `updatedAt` chưa đổi so với giá trị vừa gửi, để không xoá mất bản ghi mới hơn.
- Lỗi mạng: giữ dòng lại và đưa `SyncWorker` vào hàng đợi.

**Dấu trang:** khi tạo dấu trang, nếu lỗi mạng hoặc đang offline thì lưu vào `pending_bookmark` và báo "Đã lưu dấu trang, sẽ đồng bộ khi có mạng". Lỗi 4xx từ server thì báo lỗi như cũ, không xếp hàng.

**`SyncWorker`:** unique name `sync-{ownerKey}`, policy `KEEP`, ràng buộc mạng, backoff mũ. Được đưa vào hàng đợi khi:
- tạo bản ghi `pending_*`;
- app khởi động và có token, nếu còn bản ghi pending.

Worker gửi lần lượt từng dòng `pending_progress`, rồi từng dòng `pending_bookmark` theo `createdAt`:
- 2xx: xoá dòng đó.
- 400 (ví dụ `PUT history` báo chương không thuộc truyện), 404 (chương hoặc truyện đã bị xoá) hoặc 409 (đã đủ 500 dấu trang): xoá dòng và ghi một thông báo lỗi đồng bộ để hiển thị ở lần mở tab Dấu trang kế tiếp. *(Quyết định của Claude: dùng Toast hoặc một banner nhỏ.)*
- Lỗi mạng hoặc 5xx: dừng và trả `Result.retry()`.
- 401: trả `Result.failure()` và **giữ nguyên** dữ liệu pending, để đăng nhập lại cùng email thì vẫn gửi tiếp.

**Chống gửi trùng dấu trang:** mỗi dòng được xoá ngay sau khi server trả 2xx, trong cùng worker. Unique work `KEEP` đảm bảo không có hai worker chạy song song. Nếu app bị tắt đúng lúc giữa "server nhận" và "xoá dòng" thì có thể trùng một dấu trang. Rủi ro này rất nhỏ nên được chấp nhận, vì backend không hỗ trợ idempotency key.

**Hiển thị:** tab "Dấu trang" gộp dấu trang từ server với các dòng `pending_bookmark` (nhãn "Chờ đồng bộ", không có nút xoá). Khi offline thì chỉ hiện các dòng pending, kèm thông báo "Đang offline".

### 5.7 Tab "Đã tải" (`LibraryFragment`)

- Thêm tab thứ 4 "Đã tải". Dữ liệu lấy hoàn toàn từ Room, không gọi mạng. Phần nội dung của tab nằm trong một `DownloadsFragment` riêng để `OfflineLibraryActivity` (5.2) dùng lại được.
- **Cấp 1:** danh sách truyện đã tải, gồm ảnh bìa (Glide, có placeholder), tên truyện, "x chương · y KB/MB" và "Cả truyện" nếu x bằng `totalChapters`.
- **Cấp 2** (bấm vào truyện): danh sách chương đã tải sắp theo `chapterNo`. Bấm chương thì mở `ReaderActivity`, truyền `chapter_ids/numbers/titles` của các chương đã tải và `char_offset` lấy từ `local_progress` nếu có. Cách hiện cấp 2 (Activity mới, BottomSheet hay đổi nội dung ngay trong tab) để người xây dựng chọn.
- **Xoá:** nhấn giữ một truyện hoặc chương, chọn "Xoá", xác nhận.
- `MainActivity.openLibraryTab` dùng được với chỉ số tab mới.

### 5.8 Khi mất mạng ở các màn khác

- **Trang chủ** (`MainActivity.loadStories`): khi `onFailure` vì lỗi mạng, hiện thông báo trống "Không có kết nối mạng" kèm nút "Xem truyện đã tải", nút này gọi `openLibraryTab(<index tab Đã tải>)`.
- **Chi tiết truyện:** khi offline, nếu truyện đã có trong `downloaded_story` thì hiện thông tin từ Room và danh sách chương đã tải; nếu chưa có thì báo lỗi như hiện tại. *(Quyết định của Claude, để không có ngõ cụt khi offline.)*
- Phát hiện offline bằng `onFailure` của Retrofit (IOException). Không cần lắng nghe trạng thái mạng liên tục.

## 6. Ràng buộc

- Java 11, minSdk 24, targetSdk 37, ViewBinding. Phụ thuộc mới khai báo trong `gradle/libs.versions.toml`: `androidx.room:room-runtime` + `room-compiler` (dùng `annotationProcessor`), và `androidx.work:work-runtime`. Chọn bản ổn định mới nhất tương thích với AGP 9.3.
- Không sửa backend. Dùng các endpoint có sẵn: `chapters/{id}`, `chapters/batch` (≤20), `stories/{id}/chapters` (size ≤ 50), `history`, `bookmarks`, `auth/me`.
- Không truy cập Room trên main thread. Dùng `Executor` hoặc LiveData.
- Thông báo cho người dùng bằng tiếng Việt, theo văn phong hiện có.

## 7. Kiểm thử (tối thiểu)

- **Unit test (JVM):** chia id thành lô 20; lọc chương cần tải theo `updatedAt`; quyết định của `SyncWorker` cho từng mã HTTP (2xx xoá, 404/409 xoá kèm báo lỗi, 5xx/IO retry, 401 failure và giữ dữ liệu).
- **Instrumented test:** các DAO của Room (lọc theo `ownerKey`, upsert `pending_progress`, xoá truyện khi hết chương).
- **Kiểm tra thủ công trên emulator:** tải 1 chương, bật chế độ máy bay, tắt hẳn app, mở lại. App phải vào thẳng màn chính, tab "Đã tải" có chương đó và đọc được. Tạo dấu trang khi offline, bật mạng lại, dấu trang phải xuất hiện trên server.
- **Không còn token:** đăng xuất, bật chế độ máy bay. Màn đăng nhập phải có nút "Đọc truyện đã tải", vào đọc được và không bị đẩy về màn đăng nhập. Tạo dấu trang, đăng nhập lại đúng tài khoản khi có mạng, dấu trang phải lên server. Đăng nhập tài khoản khác thì không được thấy hoặc gửi dấu trang đó.

## 8. Những phần để người xây dựng tự quyết

- Cách hiển thị cấp 2 của tab "Đã tải".
- Icon và layout của trạng thái tải trên `item_chapter.xml`.
- Cách ước lượng dung lượng trong hộp thoại "Tải cả truyện" (có thể chỉ hiện số chương).
- Tổ chức package (ví dụ `offline/` gồm db, dao, worker, repository).
