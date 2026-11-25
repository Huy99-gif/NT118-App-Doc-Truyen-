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

    /** Sau từng này lần chạy lại mà server vẫn lỗi 5xx thì bỏ bản ghi để không chặn cả hàng đợi. */
    public static final int MAX_RETRIES = 5;

    public static SyncOutcome withAttempts(SyncOutcome outcome, int runAttemptCount) {
        return outcome == RETRY && runAttemptCount >= MAX_RETRIES ? DROP : outcome;
    }

    public static SyncOutcome fromHttpCode(int code) {
        if (code >= 200 && code < 300) return DONE;
        if (code == 401) return STOP_AUTH;
        if (code >= 400 && code < 500) return DROP;
        return RETRY;
    }
}
