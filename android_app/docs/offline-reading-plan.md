# Đọc truyện offline — Kế hoạch triển khai

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Người dùng chủ động tải từng chương hoặc cả truyện, đọc được khi không có mạng (kể cả khi không còn token), và vị trí đọc cùng dấu trang tạo lúc offline được đồng bộ khi có mạng.

**Architecture:** Room (`OfflineDatabase`, một `OfflineDao`) lưu chương, truyện, vị trí đọc và các hàng đợi chờ gửi, lọc theo `ownerKey` (email). WorkManager chạy ba worker: tải một chương, tải cả truyện, đồng bộ. Các màn hiện có (Reader, StoryDetail, Library, Login, Main) đọc từ Room trước hoặc khi mạng lỗi. Logic thuần Java (chia lô, chọn chương cần tải, phân loại mã HTTP, định dạng dung lượng) tách riêng để unit test.

**Tech Stack:** Java 11, Android SDK 24–37, ViewBinding, Retrofit 2.11 + Gson, Room (`annotationProcessor`), WorkManager, JUnit 4, AndroidX Test.

**Spec:** `android_app/docs/offline-reading-design.md`. Người thực thi đọc cả hai file.

## Global Constraints

- Không sửa backend `truyenapi`. Chỉ dùng: `GET chapters/{id}`, `GET chapters/batch?ids=` (≤ 20 id), `GET stories/{id}/chapters?page&size` (size ≤ 50), `PUT history`, `POST bookmarks`, `GET auth/me`.
- Mọi truy vấn Room lọc theo `ownerKey` = email đã `trim().toLowerCase(Locale.ROOT)`.
- Không truy cập Room trên main thread: dùng `OfflineExecutors.io()` hoặc LiveData.
- Không bật `fallbackToDestructiveMigration`. DB version = 1, `exportSchema = false`.
- Chuỗi hiển thị tiếng Việt, theo văn phong hiện có. Các chuỗi cố định do spec quy định:
  - `"Chương này chưa được tải. Cần kết nối mạng để đọc."`
  - `"Cần kết nối mạng để xem bình luận"`
  - `"Đã lưu dấu trang, sẽ đồng bộ khi có mạng"`
  - `"Kết nối mạng một lần để bật tải offline"`
  - `"Không có kết nối mạng"`, `"Xem truyện đã tải"`, `"Đọc truyện đã tải"`, `"Chờ đồng bộ"`, `"Đang offline"`, `"Tải cả truyện"`, `"Huỷ tải (x/N)"`, `"Xoá chương đã tải?"`
- Phụ thuộc mới khai báo trong `gradle/libs.versions.toml`, chọn bản ổn định mới nhất trên Google Maven tương thích AGP 9.3: `androidx.room:room-runtime`, `androidx.room:room-compiler` (annotationProcessor), `androidx.work:work-runtime`. Test thêm `androidx.room:room-testing` (androidTest) nếu cần.
- Lệnh build và test chạy trong `android_app/`:
  - unit test: `./gradlew :app:testDebugUnitTest`
  - instrumented (cần emulator): `./gradlew :app:connectedDebugAndroidTest`
  - build: `./gradlew :app:assembleDebug`

## Review Focus

1. **Truyện có hơn 50 chương khi "Tải cả truyện"**: phải lấy đủ mọi trang danh sách chương, không dừng ở trang 0. Test `DownloadPlannerTest.pageCount_*` ở Task 2.
2. **Mã HTTP 4xx ngoài 400/404/409 (vd 403, 422) khi đồng bộ**: không được thử lại mãi. Bỏ bản ghi và ghi lỗi giống 404. Test `SyncOutcomeTest.other4xx_dropsRecord` ở Task 2.
3. **Hai tài khoản trên cùng máy**: tài khoản B không thấy chương và dấu trang chờ gửi của A, và `SyncWorker` của B không gửi dòng của A. Test `OfflineDaoTest.queries_areScopedByOwner` ở Task 1 và kiểm tra thủ công ở Task 9.
4. **Ghi vị trí đọc mới trong lúc đang gửi bản cũ**: bản mới không được bị xoá. Test `OfflineDaoTest.deletePendingProgressIfUnchanged_keepsNewerRow` ở Task 1.
5. **Xoá chương cuối của một truyện**: truyện biến mất khỏi tab "Đã tải", không để lại dòng "0 chương". Test `OfflineDaoTest.deleteLastChapter_removesStory` ở Task 1.

---

### Task 1: Phụ thuộc + schema Room + DAO

**Files:**
- Modify: `android_app/gradle/libs.versions.toml`, `android_app/app/build.gradle.kts`
- Create (package `com.example.android_app.offline.db`): `DownloadedStoryEntity.java`, `DownloadedChapterEntity.java`, `LocalProgressEntity.java`, `PendingProgressEntity.java`, `PendingBookmarkEntity.java`, `ChapterHeader.java`, `DownloadedStorySummary.java`, `OfflineDao.java`, `OfflineDatabase.java`
- Create: `app/src/main/java/com/example/android_app/offline/OfflineExecutors.java`
- Test: `app/src/androidTest/java/com/example/android_app/offline/db/OfflineDaoTest.java`

**Interfaces — Produces:**

Entity: public fields, composite primary key theo spec 5.3. Tên bảng như spec.
- `DownloadedStoryEntity` (`downloaded_story`): `@NonNull String ownerKey; long storyId; String title; String authorName; String coverUrl; int totalChapters; long downloadedAt`. PK (`ownerKey`, `storyId`).
- `DownloadedChapterEntity` (`downloaded_chapter`): `@NonNull String ownerKey; long chapterId; long storyId; int chapterNo; String title; String content; String updatedAt; long downloadedAt; long sizeBytes`. PK (`ownerKey`, `chapterId`), index (`ownerKey`, `storyId`).
  - Factory: `static DownloadedChapterEntity from(String ownerKey, ChapterDetailResponse c, long storyIdFallback, long now)`, với `sizeBytes = content` theo UTF-8.
- `LocalProgressEntity` (`local_progress`) và `PendingProgressEntity` (`pending_progress`): `@NonNull String ownerKey; long storyId; long chapterId; int charOffset; long updatedAt`. PK (`ownerKey`, `storyId`).
- `PendingBookmarkEntity` (`pending_bookmark`): `@PrimaryKey(autoGenerate = true) long localId; @NonNull String ownerKey; long storyId; long chapterId; int chapterNo; String chapterTitle; String storyTitle; int charOffset; String note; long createdAt`.
- POJO `ChapterHeader`: `long chapterId; long storyId; int chapterNo; String title; String updatedAt; long sizeBytes`. Không có `content`.
- POJO `DownloadedStorySummary`: `long storyId; String title; String authorName; String coverUrl; int totalChapters; int chapterCount; long totalBytes`.

`public abstract class OfflineDao` (Room `@Dao`):
- `@Upsert void upsertStory(DownloadedStoryEntity)`
- `@Upsert void upsertChapters(List<DownloadedChapterEntity>)`
- `DownloadedChapterEntity findChapter(String ownerKey, long chapterId)`
- `DownloadedStoryEntity findStory(String ownerKey, long storyId)`
- `List<ChapterHeader> chapterHeaders(String ownerKey, long storyId)` (ORDER BY chapterNo)
- `LiveData<List<ChapterHeader>> observeChapterHeaders(String ownerKey, long storyId)`
- `LiveData<List<DownloadedStorySummary>> observeStories(String ownerKey)` (JOIN + GROUP BY, ORDER BY downloadedAt DESC)
- `int countChapters(String ownerKey)`
- `@Transaction void deleteChapter(String ownerKey, long storyId, long chapterId)`: xoá chương, rồi xoá `downloaded_story` nếu truyện không còn chương nào.
- `@Transaction void deleteStory(String ownerKey, long storyId)`
- `@Upsert void upsertLocalProgress(LocalProgressEntity)`; `LocalProgressEntity findLocalProgress(String ownerKey, long storyId)`
- `@Upsert void upsertPendingProgress(PendingProgressEntity)`; `List<PendingProgressEntity> pendingProgress(String ownerKey)`
- `int deletePendingProgressIfUnchanged(String ownerKey, long storyId, long updatedAt)`: chỉ xoá khi `updatedAt` khớp.
- `long insertPendingBookmark(PendingBookmarkEntity)`; `List<PendingBookmarkEntity> pendingBookmarks(String ownerKey)` (ORDER BY createdAt); `void deletePendingBookmark(long localId)`
- `int countPending(String ownerKey)` = số dòng pending_progress + pending_bookmark.

`OfflineDatabase`: `static OfflineDatabase get(Context)` (singleton, file `offline.db`), `abstract OfflineDao dao()`.

`OfflineExecutors`: `static Executor io()` (một single-thread executor dùng chung), `static void main(Runnable)` (post lên main looper).

- [ ] **Step 1: Thêm phụ thuộc** Room runtime + compiler (`annotationProcessor(libs.room.compiler)`), WorkManager runtime, AndroidX test core cho androidTest. Chạy `./gradlew :app:assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 2: Viết test thất bại** `OfflineDaoTest` (DB in-memory qua `Room.inMemoryDatabaseBuilder`, `allowMainThreadQueries()`):
  - `queries_areScopedByOwner`: chèn chương 1 cho `"a@x.com"`, chương 2 cho `"b@x.com"`. `findChapter("b@x.com", 1)` phải là `null`; `countChapters("a@x.com") == 1`; `pendingBookmarks("b@x.com")` không chứa dấu trang của a.
  - `deleteLastChapter_removesStory`: một truyện có 2 chương. Xoá chương 1 thì `findStory != null`; xoá tiếp chương 2 thì `findStory == null`.
  - `deletePendingProgressIfUnchanged_keepsNewerRow`: upsert với `updatedAt = 100`, upsert lại với `updatedAt = 200`. `deletePendingProgressIfUnchanged(owner, story, 100)` trả `0` và dòng vẫn còn; gọi với `200` thì trả `1`.
  - `chapterHeaders_orderedByChapterNo_withoutContent`: chèn chương số 3 rồi 1, kết quả theo thứ tự [1, 3].
  - `observeStories_countsChaptersAndBytes`: 2 chương có sizeBytes 100 và 50, kết quả `chapterCount == 2`, `totalBytes == 150`. Dùng `InstantTaskExecutorRule` hoặc `getValue` qua observer.
- [ ] **Step 3: Chạy test** `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.android_app.offline.db.OfflineDaoTest`. Expected: FAIL do chưa biên dịch được.
- [ ] **Step 4: Viết entity, POJO, DAO, DB, OfflineExecutors** theo Interfaces trên.
- [ ] **Step 5: Chạy lại test.** Expected: 5 test PASS.
- [ ] **Step 6: Commit** `feat(android): Room lưu truyện và chương offline`

---

### Task 2: Logic thuần Java (chia lô, chọn chương, phân loại mã HTTP, dung lượng)

**Files:**
- Create (package `com.example.android_app.offline`): `ChapterBatcher.java`, `DownloadPlanner.java`, `SyncOutcome.java`, `StorageFormat.java`
- Test: `app/src/test/java/com/example/android_app/offline/` gồm `ChapterBatcherTest.java`, `DownloadPlannerTest.java`, `SyncOutcomeTest.java`, `StorageFormatTest.java`

**Interfaces — Produces:**
- `ChapterBatcher`: `static final int MAX_BATCH = 20`; `static List<List<Long>> chunk(List<Long> ids, int size)`
- `DownloadPlanner`:
  - `static final int PAGE_SIZE = 50`
  - `static int pageCount(long totalElements, int pageSize)`
  - `static List<Long> chaptersToFetch(List<ChapterSummaryResponse> remote, Map<Long, String> localUpdatedAt)`: bỏ qua phần tử có `id == null`; lấy chương chưa có ở máy, hoặc có rồi nhưng `updatedAt` khác (so sánh bằng `Objects.equals`); giữ thứ tự đầu vào.
- `enum SyncOutcome { DONE, DROP, RETRY, STOP_AUTH }`; `static SyncOutcome fromHttpCode(int code)`: 2xx → DONE; 401 → STOP_AUTH; 4xx khác (gồm 400/404/409) → DROP; 5xx và còn lại → RETRY. Lỗi IOException thì người gọi tự xử lý là RETRY.
- `StorageFormat`: `static String formatBytes(long bytes)`: `< 1024` → `"N B"`; `< 1 MB` → `"N KB"` (làm tròn xuống); còn lại → `"N.N MB"` (một chữ số thập phân, dấu chấm, `Locale.ROOT`).

- [ ] **Step 1: Viết test thất bại:**
  - `chunk_45ids_gives20_20_5`
  - `chunk_empty_givesEmpty`
  - `pageCount_120_is3`, `pageCount_0_is0`, `pageCount_50_is1`
  - `chaptersToFetch_skipsSameUpdatedAt_keepsChangedAndMissing`: remote [1:"a", 2:"b", 3:"c"], local {1:"a", 2:"old"} → [2, 3]
  - `chaptersToFetch_ignoresNullId`
  - `fromHttpCode`: 200, 204 → DONE; 401 → STOP_AUTH; 400, 404, 409 → DROP; `other4xx_dropsRecord`: 403, 422 → DROP; 500, 503 → RETRY
  - `formatBytes`: 512 → `"512 B"`, 348160 → `"340 KB"`, 2202009 → `"2.1 MB"`
- [ ] **Step 2: Chạy** `./gradlew :app:testDebugUnitTest`. Expected: FAIL (lỗi biên dịch).
- [ ] **Step 3: Viết 4 class** theo Interfaces.
- [ ] **Step 4: Chạy lại.** Expected: PASS.
- [ ] **Step 5: Commit** `feat(android): logic chia lô, chọn chương cần tải và phân loại lỗi đồng bộ`

---

### Task 3: Định danh tài khoản và khởi động không cần đăng nhập lại

**Files:**
- Modify: `AuthPreferences.java`, `LoginActivity.java:44-62` (onCreate) và đoạn xử lý đăng nhập thành công (~`:139`), `MainActivity.java` (onCreate)
- Test: `app/src/androidTest/java/com/example/android_app/AuthPreferencesTest.java`

**Interfaces — Produces:**
- `AuthPreferences.saveOwnerKey(Context, String email)`: lưu `email.trim().toLowerCase(Locale.ROOT)`; bỏ qua nếu null hoặc rỗng.
- `AuthPreferences.getOwnerKey(Context) -> String` (có thể null).
- `AuthPreferences.clear` **không** xoá owner key (giữ nguyên hành vi chỉ xoá token).

- [ ] **Step 1: Viết test thất bại** `AuthPreferencesTest`:
  - `saveOwnerKey_normalizes`: `" A@X.com "` → `"a@x.com"`.
  - `clear_keepsOwnerKey_removesToken`.
- [ ] **Step 2: Chạy instrumented test.** Expected: FAIL.
- [ ] **Step 3: Viết `saveOwnerKey`/`getOwnerKey`.**
- [ ] **Step 4: Chạy lại.** Expected: PASS.
- [ ] **Step 5: Đăng nhập thành công** (`LoginActivity`): gọi `AuthPreferences.saveOwnerKey(this, loginResponse.getEmail())` ngay sau `ApiClient.onLoginSuccess`.
- [ ] **Step 6: Bỏ qua form khi đã có token:** đầu `LoginActivity.onCreate`, sau khi tính `sessionExpired`: nếu `!sessionExpired && AuthPreferences.getToken(this) != null` thì `startActivity(MainActivity)`, `finish()`, `return` (trước `setContentView`, không xin quyền mạng cục bộ).
- [ ] **Step 7: Bổ sung owner key cho máy cũ:** trong `MainActivity.onCreate`, nếu có token mà `getOwnerKey == null` thì gọi `ApiClient.auth().getCurrentUser(auth)` và lưu `email` khi thành công. Lỗi thì bỏ qua (thử lại ở lần mở app sau).
- [ ] **Step 8: Kiểm tra thủ công:** đăng nhập, tắt hẳn app, mở lại, phải vào thẳng màn chính. Đăng xuất rồi mở lại, phải hiện form.
- [ ] **Step 9: Commit** `feat(android): nhớ phiên đăng nhập và lưu email làm khoá dữ liệu offline`

---

### Task 4: Worker tải chương và tải cả truyện

**Files:**
- Create (package `com.example.android_app.offline.work`): `OfflineWork.java`, `ChapterDownloadWorker.java`, `StoryDownloadWorker.java`

**Interfaces:**
- Consumes: `OfflineDao` (Task 1), `ChapterBatcher`, `DownloadPlanner` (Task 2), `ApiClient.api()`, `ApiClient.authorization(ctx)`.
- Produces `OfflineWork`:
  - Key của `inputData`: `KEY_OWNER`, `KEY_STORY_ID`, `KEY_CHAPTER_ID`, `KEY_TITLE`, `KEY_AUTHOR`, `KEY_COVER`, `KEY_TOTAL`. Key tiến độ: `PROGRESS_DONE`, `PROGRESS_TOTAL`.
  - `static String storyTag(String owner, long storyId)` → `"story-" + owner + "-" + storyId` (gắn cho cả worker chương lẫn worker truyện).
  - `static String chapterTag(long chapterId)` → `"chapter-" + chapterId`
  - `static String storyWorkName(String owner, long storyId)` → `"download-story-" + owner + "-" + storyId`
  - `static String chapterWorkName(String owner, long chapterId)` → `"download-chapter-" + owner + "-" + chapterId`
  - `static String syncWorkName(String owner)` → `"sync-" + owner`
  - `static void enqueueChapterDownload(Context, String owner, DownloadedStoryEntity storyMeta, long chapterId)`: unique, KEEP, ràng buộc CONNECTED, gắn tag storyTag + chapterTag.
  - `static void enqueueStoryDownload(Context, String owner, DownloadedStoryEntity storyMeta)`: unique, KEEP, CONNECTED, tag storyTag, backoff mũ.
  - `static void cancelStoryDownload(Context, String owner, long storyId)`
  - `static void enqueueSync(Context, String owner)`: viết thân hàm ở Task 6. Task này chỉ khai báo chữ ký và để thân rỗng có ghi chú `// Task 6`.
- `ChapterDownloadWorker.doWork()`:
  - `getChapter(auth, chapterId).execute()`.
  - 2xx: trong một transaction, `upsertStory(meta)` rồi `upsertChapters([DownloadedChapterEntity.from(...)])`.
  - Phân loại lỗi qua `SyncOutcome.fromHttpCode`: RETRY → `Result.retry()`, còn lại → `Result.failure()`. `IOException` → `retry()`. Không có token → `failure()`.
- `StoryDownloadWorker.doWork()`:
  - Lặp các trang `getStoryChapters(auth, storyId, page, PAGE_SIZE)` cho tới `pageCount(totalElements, PAGE_SIZE)`.
  - `chaptersToFetch(remote, local updatedAt từ chapterHeaders)`, rồi `chunk(…, MAX_BATCH)`.
  - Với mỗi lô: `getChaptersBatch(auth, joined ids)`, ghi Room ngay, rồi `setProgressAsync(done, total)`.
  - Cập nhật `totalChapters` của truyện theo `totalElements`.
  - Kiểm tra `isStopped()` giữa các lô.
  - Xử lý lỗi giống `ChapterDownloadWorker`.

- [ ] **Step 1: Viết `OfflineWork`, hai worker.**
- [ ] **Step 2: Chạy** `./gradlew :app:assembleDebug :app:testDebugUnitTest`. Expected: BUILD SUCCESSFUL, test cũ vẫn PASS.
- [ ] **Step 3: Commit** `feat(android): worker tải chương và tải cả truyện`

(Phần kiểm tra chạy thực tế của worker nằm ở Task 5, khi đã có nút bấm trên giao diện.)

---

### Task 5: Giao diện tải ở màn Chi tiết truyện

**Files:**
- Modify: `adapter/ChapterAdapter.java`, `res/layout/item_chapter.xml`, `res/layout/activity_story_detail.xml`, `StoryDetailActivity.java`
- Create: `res/drawable/ic_download.xml`, `ic_download_done.xml`, `ic_downloading.xml` (vector, kiểu dáng do builder chọn)

**Interfaces:**
- Consumes: `OfflineDao.observeChapterHeaders`, `findStory`, `chapterHeaders`, `deleteChapter` (Task 1); `OfflineWork.*` (Task 4); `AuthPreferences.getOwnerKey` (Task 3).
- Produces trong `ChapterAdapter`:
  - `enum DownloadState { NONE, QUEUED, DONE }`
  - `void setDownloadStates(Map<Long, DownloadState>)`
  - `OnChapterClickListener` thêm `void onDownloadClick(Chapter chapter, DownloadState state)`
  - `item_chapter.xml`: thay `ivChevron` bằng `ImageButton btnDownload`.

- [ ] **Step 1: Adapter + layout:** nút ⬇/⏳/✓ theo trạng thái; bấm nút gọi `onDownloadClick`, bấm cả dòng vẫn gọi `onChapterClick`.
- [ ] **Step 2: Trạng thái:**
  - `observeChapterHeaders(owner, storyId)` → các id DONE.
  - `WorkManager.getWorkInfosByTagLiveData(storyTag)`: worker chương đang ENQUEUED/RUNNING (lấy id từ tag `chapter-*`) → QUEUED.
  - Gộp hai nguồn rồi gọi `setDownloadStates`.
- [ ] **Step 3: Thao tác:**
  - NONE → `enqueueChapterDownload`.
  - DONE → hộp thoại `"Xoá chương đã tải?"` → `deleteChapter` trên `OfflineExecutors.io()`.
  - QUEUED → bỏ qua.
- [ ] **Step 4: Nút `btnDownloadAll` "Tải cả truyện"** trong `activity_story_detail.xml`:
  - Bấm → hộp thoại xác nhận "Tải N chương?" (N = `chapterCount`) → `enqueueStoryDownload`.
  - Theo dõi `getWorkInfosForUniqueWorkLiveData(storyWorkName)`: khi RUNNING thì đổi chữ thành `"Huỷ tải (x/N)"` (đọc PROGRESS_*), bấm thì `cancelStoryDownload`.
  - Metadata truyện lấy từ các trường `storyName`, `storyAuthor`, `storyCover`, `chapterCount`.
- [ ] **Step 5: Thiếu owner key:** nếu `getOwnerKey == null` thì nút tải và `btnDownloadAll` bị vô hiệu, bấm vào hiện `"Kết nối mạng một lần để bật tải offline"`.
- [ ] **Step 6: Offline:**
  - `loadStoryDetail.onFailure` → `findStory`: nếu có thì điền tên, tác giả, ảnh bìa từ Room.
  - `loadChapters.onFailure` → `chapterHeaders`: nếu có thì điền danh sách chương, tất cả ở trạng thái DONE.
  - Nếu cả hai đều trống thì giữ toast lỗi như hiện tại.
- [ ] **Step 7: Kiểm tra thủ công trên emulator:**
  - Tải 1 chương: nút chuyển ⏳ rồi ✓.
  - Truyện có hơn 50 chương: "Tải cả truyện" phải tải đủ toàn bộ (đếm ở Task 8 hoặc bằng App Inspection > Database).
  - Bấm huỷ giữa chừng: các chương đã tải được giữ lại.
  - Bật chế độ máy bay rồi mở lại màn: thấy thông tin và các chương đã tải.
- [ ] **Step 8: Commit** `feat(android): tải và xoá chương offline ở màn chi tiết truyện`

---

### Task 6: Đồng bộ vị trí đọc và dấu trang (SyncWorker + ghi vào hàng đợi)

**Files:**
- Create: `offline/work/SyncWorker.java`, `offline/SyncErrorStore.java`
- Modify: `offline/work/OfflineWork.java` (thân `enqueueSync`), `App.java` (gọi đồng bộ lúc khởi động), `model/BookmarkResponse.java`, `adapter/BookmarkAdapter.java`, `LibraryFragment.java` (`loadBookmarks`)

**Interfaces:**
- Consumes: `OfflineDao` các hàm pending (Task 1), `SyncOutcome` (Task 2), `syncWorkName` (Task 4).
- Produces:
  - `SyncErrorStore.record(Context, String message)` và `String consume(Context)`: prefs `"offline_sync"`, nối nhiều lỗi bằng `"\n"`, `consume` đọc xong thì xoá.
  - `OfflineWork.enqueueSync`: unique `syncWorkName`, KEEP, CONNECTED, backoff mũ, `inputData` có KEY_OWNER.
  - `BookmarkResponse`: thêm `public transient boolean pending; public transient long localId;` (transient để Gson bỏ qua).
- `SyncWorker.doWork()`:
  - Nếu `owner` khác `getOwnerKey()` hoặc không có token → `Result.failure()`, giữ nguyên dữ liệu.
  - Gửi từng `pendingProgress` qua `PUT history`: DONE → `deletePendingProgressIfUnchanged(…, row.updatedAt)`; DROP → xoá và `record(...)`; RETRY/IOException → `Result.retry()`; STOP_AUTH → `Result.failure()`.
  - Sau đó gửi `pendingBookmarks` qua `POST bookmarks` theo cùng luật (DONE/DROP → `deletePendingBookmark`).
  - Nội dung lỗi: `"Không đồng bộ được dấu trang chương N: <message từ ApiErrorParser>"`. Vị trí đọc tương tự.
- `App.onCreate`: nếu có token và owner key thì `OfflineExecutors.io()` kiểm tra `countPending(owner) > 0` rồi gọi `enqueueSync`.
- `LibraryFragment.loadBookmarks`:
  - Gộp danh sách server với `pendingBookmarks(owner)` (map sang `BookmarkResponse` có `pending = true`, xếp lên đầu).
  - Khi request lỗi IOException thì chỉ hiện các dòng pending, kèm `"Đang offline"` ở `tvEmpty` hoặc toast.
  - Đầu hàm gọi `SyncErrorStore.consume` và hiện toast nếu có lỗi.
  - `BookmarkAdapter`: dòng pending hiện `"Chờ đồng bộ"` và ẩn nút xoá.

- [ ] **Step 1: Viết `SyncErrorStore`, `SyncWorker`, `enqueueSync`, phần gọi ở `App`.**
- [ ] **Step 2: Sửa `BookmarkResponse`, `BookmarkAdapter`, `LibraryFragment.loadBookmarks`.**
- [ ] **Step 3: Chạy** `./gradlew :app:assembleDebug :app:testDebugUnitTest`. Expected: PASS.
- [ ] **Step 4: Commit** `feat(android): đồng bộ vị trí đọc và dấu trang tạo lúc offline`

(Kiểm tra thực tế ở Task 7, khi Reader đã ghi vào hàng đợi.)

---

### Task 7: ReaderActivity: ưu tiên bản trong máy, chế độ offline, ghi hàng đợi

**Files:**
- Modify: `ReaderActivity.java`

**Interfaces:**
- Consumes: `OfflineDao.findChapter`, `chapterHeaders`, `upsertLocalProgress`, `upsertPendingProgress`, `deletePendingProgressIfUnchanged`, `insertPendingBookmark`, `findStory` (Task 1); `OfflineWork.enqueueSync` (Task 6); `AuthPreferences.getOwnerKey` (Task 3).
- Produces: `public static final String EXTRA_OFFLINE_MODE = "offline_mode";`. Các extra khác giữ tên hiện có (`story_id`, `story_name`, `chapter_id`, `chapter_number`, `chapter_title`, `char_offset`, `chapter_ids`, `chapter_numbers`, `chapter_titles`).

- [ ] **Step 1: Tách phần hiển thị** thành `renderChapter(long id, long storyId, int chapterNo, String title, String content, String updatedAt)`, dùng chung cho cả nhánh mạng và nhánh cục bộ.
- [ ] **Step 2: `loadChapterContent`:**
  - Có owner thì tra `findChapter` trên io. Có bản cục bộ thì `renderChapter` và không gọi mạng.
  - Không có bản cục bộ: nếu `offlineMode` hoặc không có token thì hiện `"Chương này chưa được tải. Cần kết nối mạng để đọc."`; nếu không thì gọi API như cũ, và `onFailure` (IOException) cũng hiện thông báo đó.
  - Kiểm tra `authorization == null` chỉ còn áp dụng cho nhánh mạng.
  - Huỷ kết quả cũ nếu người dùng đã chuyển sang chương khác (so sánh `id` với `chapterId` hiện tại khi callback về).
- [ ] **Step 3: Danh sách chương:** khi `offlineMode`, khi `onFailure`, hoặc khi chưa có danh sách, dựng `chapterIds/Numbers/Titles` từ `chapterHeaders(owner, storyId)`.
- [ ] **Step 4: `saveReadingProgress`:**
  - Trước tiên ghi `local_progress` và `pending_progress` (cùng `updatedAt = System.currentTimeMillis()`) trên io.
  - Sau đó, nếu không ở `offlineMode` và có token thì gọi API như cũ. Thành công → `deletePendingProgressIfUnchanged(…, updatedAt đã ghi)`; IOException → `enqueueSync`.
  - Giữ nguyên logic `historyRequestInFlight`/`historyUpdatePending`. Bỏ điều kiện `authorization == null` ở đầu hàm, chỉ cần có owner.
- [ ] **Step 5: `createBookmark`:** nếu `offlineMode`, không có token, hoặc `onFailure` là IOException thì `insertPendingBookmark` (lấy storyTitle/chapterNo/chapterTitle từ trạng thái hiện tại), gọi `enqueueSync`, và hiện `"Đã lưu dấu trang, sẽ đồng bộ khi có mạng"`. Lỗi 4xx giữ nguyên hành vi cũ.
- [ ] **Step 6: Bình luận:** thêm trường `networkUnavailable`, đặt `true` khi `offlineMode` hoặc khi gặp IOException trong Reader. Nếu true thì `showComments` hiện `"Cần kết nối mạng để xem bình luận"` thay vì mở sheet.
- [ ] **Step 7: Build** `./gradlew :app:assembleDebug`. Expected: SUCCESS.
- [ ] **Step 8: Kiểm tra thủ công:**
  - Chương đã tải mở được khi bật chế độ máy bay, bấm trước/sau chỉ chuyển giữa các chương đã tải.
  - Chương chưa tải hiện đúng thông báo.
  - Tạo dấu trang khi offline thấy toast; tab Dấu trang có dòng "Chờ đồng bộ"; tắt chế độ máy bay thì dấu trang xuất hiện trên server (dòng pending biến mất).
  - Đọc khi online rồi mở lại khi offline: vẫn đúng vị trí (nhờ `local_progress`).
- [ ] **Step 9: Commit** `feat(android): đọc chương đã tải và lưu tiến độ, dấu trang khi offline`

---

### Task 8: Tab "Đã tải" (DownloadsFragment) + trạng thái offline ở Trang chủ

**Files:**
- Create: `DownloadsFragment.java`, `adapter/DownloadedStoryAdapter.java`, `res/layout/fragment_downloads.xml`, `res/layout/item_downloaded_story.xml`
- Modify: `LibraryFragment.java` (thêm tab thứ 4), `res/layout/fragment_library.xml` (nơi chứa `DownloadsFragment`), `MainActivity.java` (`loadStories.onFailure`), `res/layout/activity_main.xml` (nút `btnOpenDownloads`)

**Interfaces:**
- Consumes: `observeStories`, `chapterHeaders`, `findLocalProgress`, `deleteStory`, `deleteChapter` (Task 1); `StorageFormat.formatBytes` (Task 2); `ReaderActivity.EXTRA_OFFLINE_MODE` (Task 7).
- Produces:
  - `DownloadsFragment.newInstance(boolean offlineMode)`: khi `offlineMode = true` thì truyền `EXTRA_OFFLINE_MODE` khi mở Reader. Task 9 dùng lại.
  - `LibraryFragment.TAB_DOWNLOADS = 3`.
  - `MainActivity.openLibraryTab(LibraryFragment.TAB_DOWNLOADS)` hoạt động.

- [ ] **Step 1: `DownloadsFragment`:**
  - Cấp 1: `observeStories(owner)`. Mỗi dòng gồm ảnh bìa (Glide + placeholder `ic_launcher_foreground`), tên, `"x chương · " + formatBytes(totalBytes)`, và `" · Cả truyện"` khi `chapterCount >= totalChapters > 0`.
  - Cấp 2: bấm truyện thì hiện danh sách chương (cách hiển thị do builder chọn). Bấm chương thì mở Reader với `chapter_ids/numbers/titles` của các chương đã tải và `char_offset` từ `findLocalProgress` (chỉ khi `chapterId` khớp, không thì 0).
  - Nhấn giữ truyện hoặc chương → xác nhận → `deleteStory`/`deleteChapter`.
  - Owner null hoặc không có dữ liệu → hiện thông báo trống.
- [ ] **Step 2: `LibraryFragment`:** thêm tab `"Đã tải"`. Khi chọn tab 3 thì ẩn `rvLibrary` và hiện container chứa `DownloadsFragment.newInstance(false)` (dùng child fragment manager); khi chọn tab khác thì làm ngược lại.
- [ ] **Step 3: `MainActivity.loadStories.onFailure`:** đặt `tvEmpty` = `"Không có kết nối mạng"` và hiện `btnOpenDownloads` (`"Xem truyện đã tải"`) gọi `openLibraryTab(TAB_DOWNLOADS)`. Ẩn nút này khi tải thành công.
- [ ] **Step 4: Build** `./gradlew :app:assembleDebug`. Expected: SUCCESS.
- [ ] **Step 5: Kiểm tra thủ công:** đúng kịch bản ở spec mục 7. Tải 1 chương, bật chế độ máy bay, tắt hẳn app, mở lại: vào thẳng màn chính, Trang chủ có nút "Xem truyện đã tải", tab "Đã tải" có truyện đó và đọc được. Xoá chương cuối thì truyện biến mất khỏi danh sách.
- [ ] **Step 6: Commit** `feat(android): tab Đã tải trong tủ sách và lối vào khi mất mạng`

---

### Task 9: Đọc truyện đã tải khi không còn token

**Files:**
- Create: `OfflineLibraryActivity.java`, `res/layout/activity_offline_library.xml`
- Modify: `LoginActivity.java`, `res/layout/activity_login.xml` (nút `btnReadDownloaded`), `App.java` (`onActivityResumed`), `AndroidManifest.xml`

**Interfaces:**
- Consumes: `DownloadsFragment.newInstance(true)` (Task 8), `OfflineDao.countChapters` (Task 1), `AuthPreferences.getOwnerKey` (Task 3), `ReaderActivity.EXTRA_OFFLINE_MODE` (Task 7).
- Produces: `OfflineLibraryActivity` (toolbar có tiêu đề "Truyện đã tải" và nút "Đăng nhập" quay về `LoginActivity`).

- [ ] **Step 1: `OfflineLibraryActivity`:** chứa `DownloadsFragment.newInstance(true)`, không có bottom navigation. Khai báo trong manifest.
- [ ] **Step 2: `LoginActivity`:** nút `btnReadDownloaded` (`"Đọc truyện đã tải"`) mặc định GONE. Trong `onCreate`, nếu `getOwnerKey != null` thì kiểm tra `countChapters(owner) > 0` trên io, nếu đúng thì hiện nút. Bấm nút mở `OfflineLibraryActivity`. Áp dụng cả khi màn mở vì hết phiên.
- [ ] **Step 3: `App.onActivityResumed`:** thêm điều kiện bỏ qua chuyển hướng khi `activity instanceof OfflineLibraryActivity`, hoặc khi `activity instanceof ReaderActivity` và intent có `EXTRA_OFFLINE_MODE = true`.
- [ ] **Step 4: Build** `./gradlew :app:assembleDebug`. Expected: SUCCESS.
- [ ] **Step 5: Kiểm tra thủ công** (spec mục 7, "Không còn token"):
  - Đăng xuất, bật chế độ máy bay: màn đăng nhập có nút, vào đọc được và không bị đẩy về form.
  - Tạo dấu trang, sau đó đăng nhập lại **đúng** tài khoản khi có mạng: dấu trang lên server.
  - Lặp lại nhưng đăng nhập **tài khoản khác**: tab Dấu trang không có dấu trang đó và nó không được gửi lên (Review Focus #3).
  - Tài khoản chưa tải chương nào thì không hiện nút.
- [ ] **Step 6: Commit** `feat(android): đọc truyện đã tải từ màn đăng nhập khi không còn phiên`

---

### Task 10: Kiểm tra cuối

- [ ] **Step 1:** `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:assembleDebug`. Expected: tất cả PASS.
- [ ] **Step 2:** Đối chiếu spec mục 2 (Mục tiêu và Điều không được xảy ra): mỗi dòng phải chỉ ra được kịch bản thủ công nào đã kiểm tra nó. Ghi lại dòng nào chưa đạt.
- [ ] **Step 3:** Cập nhật `android_app/docs/offline-reading-design.md` nếu khi làm có quyết định lệch khỏi thiết kế. Commit `docs(android): cập nhật thiết kế offline theo thực tế triển khai` nếu có thay đổi.
