package com.example.android_app.offline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChapterBatcherTest {
    @Test
    public void chunk_45ids_gives20_20_5() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 45; i++) ids.add(i);

        List<List<Long>> batches = ChapterBatcher.chunk(ids, ChapterBatcher.MAX_BATCH);

        assertEquals(3, batches.size());
        assertEquals(20, batches.get(0).size());
        assertEquals(20, batches.get(1).size());
        assertEquals(5, batches.get(2).size());
        assertEquals(Long.valueOf(1), batches.get(0).get(0));
        assertEquals(Long.valueOf(45), batches.get(2).get(4));
    }

    @Test
    public void chunk_empty_givesEmpty() {
        assertTrue(ChapterBatcher.chunk(Collections.<Long>emptyList(), 20).isEmpty());
    }
}
