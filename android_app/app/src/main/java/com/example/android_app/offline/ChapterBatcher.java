package com.example.android_app.offline;

import java.util.ArrayList;
import java.util.List;

/** Chia danh sách id chương thành các lô vừa với giới hạn của GET chapters/batch. */
public final class ChapterBatcher {
    /** Backend chỉ nhận tối đa 20 id mỗi lần (ChapterService.MAX_BATCH_SIZE). */
    public static final int MAX_BATCH = 20;

    private ChapterBatcher() {}

    public static List<List<Long>> chunk(List<Long> ids, int size) {
        List<List<Long>> batches = new ArrayList<>();
        for (int start = 0; start < ids.size(); start += size) {
            batches.add(new ArrayList<>(ids.subList(start, Math.min(start + size, ids.size()))));
        }
        return batches;
    }
}
