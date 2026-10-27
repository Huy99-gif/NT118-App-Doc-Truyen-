---
applyTo: "android-app/**/*.java"
---
# Quy ước code Android (Java)

- Chỉ dùng **Java**, XML layout; không dùng Kotlin hay Jetpack Compose.
- Kiến trúc MVVM: `Fragment` hiển thị, `ViewModel` giữ trạng thái (LiveData), `Repository` quyết định lấy dữ liệu từ mạng hay Room.
- Gọi API bằng Retrofit trong `data/remote`; tuyệt đối không gọi mạng trên luồng giao diện.
- Room entity/DAO ở `data/local`; WorkManager worker ở `worker/` (`DownloadChapterWorker`, `SyncWorker`).
- Địa chỉ backend khi chạy emulator là `http://10.0.2.2:8080/`; điện thoại thật dùng IP LAN của máy chạy backend.
- JWT lưu bằng EncryptedSharedPreferences và gắn vào header `Authorization: Bearer ...` bằng OkHttp interceptor.
- Cài đặt đọc (cỡ chữ, font, Dark/Sepia) lưu bằng SharedPreferences.
- Vị trí đọc lưu theo offset ký tự/đoạn, ghi vào Room có debounce (không ghi mỗi lần cuộn).
- minSdk 26. Package: `com.nhom7.truyen`.
