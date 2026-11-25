package com.example.android_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.android_app.offline.db.DownloadedChapterEntity;
import com.example.android_app.offline.db.DownloadedStoryEntity;
import com.example.android_app.offline.db.OfflineDao;
import com.example.android_app.offline.db.OfflineDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/** Đọc chương đã tải ở chế độ offline (không token, không gọi mạng). */
@RunWith(AndroidJUnit4.class)
public class ReaderOfflineTest {
    private static final String OWNER = "reader-test@x.com";
    private static final long STORY_ID = 990001;
    private static final long CHAPTER_ID = 990011;

    private Context context;
    private OfflineDao dao;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        AuthPreferences.clear(context);
        AuthPreferences.saveOwnerKey(context, OWNER);
        dao = OfflineDatabase.get(context).dao();
        DownloadedStoryEntity story = new DownloadedStoryEntity();
        story.ownerKey = OWNER;
        story.storyId = STORY_ID;
        story.title = "Truyện thử";
        DownloadedChapterEntity chapter = new DownloadedChapterEntity();
        chapter.ownerKey = OWNER;
        chapter.storyId = STORY_ID;
        chapter.chapterId = CHAPTER_ID;
        chapter.chapterNo = 1;
        chapter.title = "Mở đầu";
        chapter.content = "Nội dung đã tải về máy.";
        dao.saveDownloaded(story, Collections.singletonList(chapter));
    }

    @After
    public void tearDown() {
        dao.deleteStory(OWNER, STORY_ID);
    }

    @Test
    public void downloadedChapter_readsWithoutNetwork() throws Exception {
        assertEquals("Nội dung đã tải về máy.", contentShownFor(CHAPTER_ID));
    }

    @Test
    public void notDownloadedChapter_showsMessage() throws Exception {
        assertEquals(context.getString(R.string.offline_chapter_not_downloaded),
                contentShownFor(CHAPTER_ID + 1));
    }

    /**
     * Đang online (có token, không offline_mode): một request lỗi mạng thoáng qua không được khoá
     * nút bình luận cho tới khi mở lại màn đọc.
     */
    @Test
    public void onlineReader_afterNetworkBlip_commentsStillOpen() throws Exception {
        DownloadedChapterEntity second = new DownloadedChapterEntity();
        second.ownerKey = OWNER;
        second.storyId = STORY_ID;
        second.chapterId = CHAPTER_ID + 1;
        second.chapterNo = 2;
        second.content = "chương 2";
        dao.upsertChapters(Collections.singletonList(second));
        AuthPreferences.saveToken(context, "token-khong-hop-le");
        Intent intent = new Intent(context, ReaderActivity.class)
                .putExtra("story_id", STORY_ID)
                .putExtra("chapter_id", CHAPTER_ID)
                .putExtra("story_name", "Truyện thử");
        try (ActivityScenario<ReaderActivity> scenario = ActivityScenario.launch(intent)) {
            // danh sách chương lấy từ máy (nút "sau" bật) nghĩa là request mạng đã lỗi
            AtomicReference<Boolean> failedOver = new AtomicReference<>(false);
            for (int i = 0; i < 250 && !failedOver.get(); i++) {
                scenario.onActivity(a -> failedOver.set(a.findViewById(R.id.btnNextChapter).isEnabled()));
                if (!failedOver.get()) Thread.sleep(100);
            }
            assertTrue("Request mạng chưa lỗi để chuyển sang danh sách trong máy", failedOver.get());

            scenario.onActivity(a -> a.findViewById(R.id.btnComments).performClick());
            Thread.sleep(500);
            AtomicReference<Boolean> sheetShown = new AtomicReference<>(false);
            scenario.onActivity(a -> sheetShown.set(a.getSupportFragmentManager()
                    .findFragmentByTag(CommentsBottomSheet.TAG) != null));
            assertTrue("Sheet bình luận không mở", sheetShown.get());
        } finally {
            AuthPreferences.clear(context);
        }
    }

    private String contentShownFor(long chapterId) throws InterruptedException {
        Intent intent = new Intent(context, ReaderActivity.class)
                .putExtra("story_id", STORY_ID)
                .putExtra("chapter_id", chapterId)
                .putExtra("story_name", "Truyện thử")
                .putExtra(ReaderActivity.EXTRA_OFFLINE_MODE, true);
        try (ActivityScenario<ReaderActivity> scenario = ActivityScenario.launch(intent)) {
            AtomicReference<String> text = new AtomicReference<>();
            // nội dung được đọc trên luồng nền nên chờ tối đa 5 giây
            for (int i = 0; i < 50; i++) {
                scenario.onActivity(activity -> {
                    TextView content = activity.findViewById(R.id.tvChapterContent);
                    text.set(content.isShown() ? content.getText().toString() : null);
                });
                if (text.get() != null) break;
                Thread.sleep(100);
            }
            assertNotNull("Nội dung chương không hiện", text.get());
            return text.get();
        }
    }
}
