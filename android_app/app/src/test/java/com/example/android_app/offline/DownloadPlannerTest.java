package com.example.android_app.offline;

import static org.junit.Assert.assertEquals;

import com.example.android_app.model.ChapterSummaryResponse;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DownloadPlannerTest {
    @Test
    public void pageCount_120_is3() {
        assertEquals(3, DownloadPlanner.pageCount(120, DownloadPlanner.PAGE_SIZE));
    }

    @Test
    public void pageCount_0_is0() {
        assertEquals(0, DownloadPlanner.pageCount(0, DownloadPlanner.PAGE_SIZE));
    }

    @Test
    public void pageCount_50_is1() {
        assertEquals(1, DownloadPlanner.pageCount(50, DownloadPlanner.PAGE_SIZE));
    }

    @Test
    public void chaptersToFetch_skipsSameUpdatedAt_keepsChangedAndMissing() {
        List<ChapterSummaryResponse> remote = Arrays.asList(
                summary(1L, "a"), summary(2L, "b"), summary(3L, "c"));
        Map<Long, String> local = new HashMap<>();
        local.put(1L, "a");
        local.put(2L, "old");

        assertEquals(Arrays.asList(2L, 3L), DownloadPlanner.chaptersToFetch(remote, local));
    }

    @Test
    public void chaptersToFetch_ignoresNullId() {
        List<ChapterSummaryResponse> remote = Arrays.asList(summary(null, "a"), summary(5L, "b"));

        assertEquals(Arrays.asList(5L), DownloadPlanner.chaptersToFetch(remote, new HashMap<>()));
    }

    private static ChapterSummaryResponse summary(Long id, String updatedAt) {
        ChapterSummaryResponse summary = new ChapterSummaryResponse();
        summary.id = id;
        summary.updatedAt = updatedAt;
        return summary;
    }
}
