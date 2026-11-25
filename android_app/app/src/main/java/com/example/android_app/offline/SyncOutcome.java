package com.example.android_app.offline;

/** Cách xử lý một bản ghi chờ gửi (hoặc một lần tải) theo mã HTTP server trả về. */
public enum SyncOutcome {
    /** Thành công: xoá bản ghi khỏi hàng đợi. */
    DONE,
    /** Server từ chối vĩnh viễn (4xx): bỏ bản ghi và báo lỗi, không thử lại. */
    DROP,
    /** Lỗi tạm thời (5xx): thử lại sau. */
    RETRY,
    /** Hết phiên (401): dừng, giữ nguyên dữ liệu chờ đăng nhập lại. */
    STOP_AUTH;

    public static SyncOutcome fromHttpCode(int code) {
        if (code >= 200 && code < 300) return DONE;
        if (code == 401) return STOP_AUTH;
        if (code >= 400 && code < 500) return DROP;
        return RETRY;
    }
}
