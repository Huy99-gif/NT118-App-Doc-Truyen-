package com.example.android_app.offline;

import com.example.android_app.model.ChapterSummaryResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Quyết định cần tải những chương nào khi "Tải cả truyện". */
public final class DownloadPlanner {
    /** Backend giới hạn size tối đa 50 mỗi trang (PageableFactory.MAX_SIZE). */
    public static final int PAGE_SIZE = 50;

    private DownloadPlanner() {}

    public static int pageCount(long totalElements, int pageSize) {
        if (totalElements <= 0) return 0;
        return (int) ((totalElements + pageSize - 1) / pageSize);
    }

    /** Chương chưa có trên máy, hoặc đã có nhưng server đã cập nhật (updatedAt khác). */
    public static List<Long> chaptersToFetch(List<ChapterSummaryResponse> remote,
                                             Map<Long, String> localUpdatedAt) {
        List<Long> result = new ArrayList<>();
        for (ChapterSummaryResponse chapter : remote) {
            if (chapter.id == null) continue;
            if (!localUpdatedAt.containsKey(chapter.id)
                    || !Objects.equals(localUpdatedAt.get(chapter.id), chapter.updatedAt)) {
                result.add(chapter.id);
            }
        }
        return result;
    }
}
