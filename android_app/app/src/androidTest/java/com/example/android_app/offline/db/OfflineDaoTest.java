package com.example.android_app.offline.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class OfflineDaoTest {
    private static final String A = "a@x.com";
    private static final String B = "b@x.com";

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private OfflineDatabase db;
    private OfflineDao dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, OfflineDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.dao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void queries_areScopedByOwner() {
        dao.upsertStory(story(A, 10));
        dao.upsertStory(story(B, 10));
        dao.upsertChapters(Arrays.asList(chapter(A, 10, 1, 1, 10)));
        dao.upsertChapters(Arrays.asList(chapter(B, 10, 2, 2, 10)));
        PendingBookmarkEntity bookmark = new PendingBookmarkEntity();
        bookmark.ownerKey = A;
        bookmark.storyId = 10;
        bookmark.chapterId = 1;
        bookmark.createdAt = 1;
        dao.insertPendingBookmark(bookmark);

        assertNull(dao.findChapter(B, 1));
        assertNotNull(dao.findChapter(A, 1));
        assertEquals(1, dao.countChapters(A));
        assertTrue(dao.pendingBookmarks(B).isEmpty());
        assertEquals(1, dao.pendingBookmarks(A).size());
    }

    @Test
    public void deleteLastChapter_removesStory() {
        dao.upsertStory(story(A, 10));
        dao.upsertChapters(Arrays.asList(chapter(A, 10, 1, 1, 10), chapter(A, 10, 2, 2, 10)));

        dao.deleteChapter(A, 10, 1);
        assertNotNull(dao.findStory(A, 10));

        dao.deleteChapter(A, 10, 2);
        assertNull(dao.findStory(A, 10));
    }

    @Test
    public void deletePendingProgressIfUnchanged_keepsNewerRow() {
        dao.upsertPendingProgress(progress(100));
        dao.upsertPendingProgress(progress(200));

        assertEquals(0, dao.deletePendingProgressIfUnchanged(A, 10, 100));
        assertEquals(1, dao.pendingProgress(A).size());
        assertEquals(1, dao.deletePendingProgressIfUnchanged(A, 10, 200));
        assertTrue(dao.pendingProgress(A).isEmpty());
    }

    @Test
    public void chapterHeaders_orderedByChapterNo_withoutContent() {
        dao.upsertStory(story(A, 10));
        dao.upsertChapters(Arrays.asList(chapter(A, 10, 3, 3, 10), chapter(A, 10, 1, 1, 10)));

        List<ChapterHeader> headers = dao.chapterHeaders(A, 10);

        assertEquals(2, headers.size());
        assertEquals(1, headers.get(0).chapterNo);
        assertEquals(3, headers.get(1).chapterNo);
    }

    @Test
    public void observeStories_countsChaptersAndBytes() {
        dao.upsertStory(story(A, 10));
        dao.upsertChapters(Arrays.asList(chapter(A, 10, 1, 1, 100), chapter(A, 10, 2, 2, 50)));

        List<DownloadedStorySummary> stories = valueOf(dao.observeStories(A));

        assertEquals(1, stories.size());
        assertEquals(2, stories.get(0).chapterCount);
        assertEquals(150, stories.get(0).totalBytes);
    }

    private static <T> T valueOf(LiveData<T> liveData) {
        AtomicReference<T> value = new AtomicReference<>();
        liveData.observeForever(value::set);
        return value.get();
    }

    private static DownloadedStoryEntity story(String owner, long storyId) {
        DownloadedStoryEntity story = new DownloadedStoryEntity();
        story.ownerKey = owner;
        story.storyId = storyId;
        story.title = "Truyện " + storyId;
        story.totalChapters = 2;
        story.downloadedAt = 1;
        return story;
    }

    private static DownloadedChapterEntity chapter(String owner, long storyId, long chapterId,
                                                   int chapterNo, long sizeBytes) {
        DownloadedChapterEntity chapter = new DownloadedChapterEntity();
        chapter.ownerKey = owner;
        chapter.storyId = storyId;
        chapter.chapterId = chapterId;
        chapter.chapterNo = chapterNo;
        chapter.title = "Chương " + chapterNo;
        chapter.content = "nội dung";
        chapter.sizeBytes = sizeBytes;
        return chapter;
    }

    private static PendingProgressEntity progress(long updatedAt) {
        PendingProgressEntity progress = new PendingProgressEntity();
        progress.ownerKey = A;
        progress.storyId = 10;
        progress.chapterId = 1;
        progress.charOffset = 5;
        progress.updatedAt = updatedAt;
        return progress;
    }
}
