package com.example.android_app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.example.android_app.offline.db.DownloadedChapterEntity;
import com.example.android_app.offline.db.DownloadedStoryEntity;
import com.example.android_app.offline.db.OfflineDao;
import com.example.android_app.offline.db.OfflineDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/** Đọc truyện đã tải khi không còn token: màn đăng nhập có lối vào, không bị đẩy về đăng nhập. */
@RunWith(AndroidJUnit4.class)
public class OfflineLibraryTest {
    private static final String OWNER = "library-test@x.com";
    private static final long STORY_ID = 880001;

    private Context context;
    private OfflineDao dao;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        AuthPreferences.clear(context);
        AuthPreferences.saveOwnerKey(context, OWNER);
        dao = OfflineDatabase.get(context).dao();
        dao.deleteStory(OWNER, STORY_ID);
    }

    @After
    public void tearDown() {
        dao.deleteStory(OWNER, STORY_ID);
        AuthPreferences.setSessionExpired(context, false);
    }

    @Test
    public void loginScreen_showsReadDownloaded_whenOwnerHasChapters() throws Exception {
        seedStory();
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            assertTrue(waitFor(scenario, a -> a.findViewById(R.id.btnReadDownloaded).getVisibility() == View.VISIBLE));
        }
    }

    @Test
    public void loginScreen_hidesReadDownloaded_whenNothingDownloaded() throws Exception {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            Thread.sleep(1000);
            AtomicReference<Integer> visibility = new AtomicReference<>();
            scenario.onActivity(a -> visibility.set(a.findViewById(R.id.btnReadDownloaded).getVisibility()));
            assertEquals(Integer.valueOf(View.GONE), visibility.get());
        }
    }

    @Test
    public void offlineLibrary_listsStory_andStaysOpenWithExpiredSession() throws Exception {
        seedStory();
        AuthPreferences.setSessionExpired(context, true);
        try (ActivityScenario<OfflineLibraryActivity> scenario =
                     ActivityScenario.launch(OfflineLibraryActivity.class)) {
            assertTrue("Không thấy truyện đã tải", waitFor(scenario, a -> {
                RecyclerView list = a.findViewById(R.id.rvDownloads);
                if (list == null || list.getChildCount() == 0) return false;
                TextView title = list.getChildAt(0).findViewById(R.id.tvDownloadedTitle);
                return "Truyện offline".equals(title.getText().toString());
            }));
            assertEquals(Lifecycle.State.RESUMED, scenario.getState());
        }
    }

    @Test
    public void offlineLibrary_deletingLastChapter_showsEmpty() throws Exception {
        seedStory();
        try (ActivityScenario<OfflineLibraryActivity> scenario =
                     ActivityScenario.launch(OfflineLibraryActivity.class)) {
            assertTrue(waitFor(scenario, a -> {
                RecyclerView list = a.findViewById(R.id.rvDownloads);
                return list != null && list.getChildCount() == 1;
            }));
            dao.deleteChapter(OWNER, STORY_ID, STORY_ID + 1);
            dao.deleteChapter(OWNER, STORY_ID, STORY_ID + 2);
            assertTrue("Danh sách không trống sau khi xoá", waitFor(scenario,
                    a -> a.findViewById(R.id.tvEmptyDownloads).getVisibility() == View.VISIBLE));
        }
    }

    /** Đăng nhập → Đọc truyện đã tải → Đăng nhập: không được để lại LoginActivity cũ phía sau. */
    @Test
    public void goLoginFromOfflineLibrary_leavesSingleLoginScreen() throws Exception {
        seedStory();
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            assertTrue(waitFor(scenario, a -> a.findViewById(R.id.btnReadDownloaded).getVisibility() == View.VISIBLE));
            scenario.onActivity(a -> a.findViewById(R.id.btnReadDownloaded).performClick());
            assertTrue("Không mở được thư viện offline",
                    waitUntil(() -> !activitiesOf(OfflineLibraryActivity.class).isEmpty()));
            Activity library = activitiesOf(OfflineLibraryActivity.class).get(0);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    library.findViewById(R.id.btnGoLogin).performClick());
            assertTrue("Thư viện offline chưa đóng",
                    waitUntil(() -> activitiesOf(OfflineLibraryActivity.class).isEmpty()));
            assertEquals(1, activitiesOf(LoginActivity.class).size());
        }
    }

    /** Các Activity loại cls còn sống (chưa bị huỷ). */
    private static List<Activity> activitiesOf(Class<?> cls) {
        List<Activity> result = new ArrayList<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Stage stage : new Stage[]{Stage.PRE_ON_CREATE, Stage.CREATED, Stage.STARTED,
                    Stage.RESUMED, Stage.PAUSED, Stage.STOPPED, Stage.RESTARTED}) {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(stage)) {
                    if (cls.isInstance(activity) && !activity.isFinishing()) result.add(activity);
                }
            }
        });
        return result;
    }

    private static boolean waitUntil(Callable<Boolean> condition) throws Exception {
        for (int i = 0; i < 50; i++) {
            if (condition.call()) return true;
            Thread.sleep(100);
        }
        return false;
    }

    private void seedStory() {
        DownloadedStoryEntity story = new DownloadedStoryEntity();
        story.ownerKey = OWNER;
        story.storyId = STORY_ID;
        story.title = "Truyện offline";
        story.totalChapters = 2;
        dao.saveDownloaded(story, Arrays.asList(chapter(1), chapter(2)));
    }

    private DownloadedChapterEntity chapter(int number) {
        DownloadedChapterEntity chapter = new DownloadedChapterEntity();
        chapter.ownerKey = OWNER;
        chapter.storyId = STORY_ID;
        chapter.chapterId = STORY_ID + number;
        chapter.chapterNo = number;
        chapter.title = "Chương " + number;
        chapter.content = "nội dung " + number;
        chapter.sizeBytes = 10;
        return chapter;
    }

    private interface Check<A> {
        boolean test(A activity);
    }

    /** Dữ liệu đọc từ Room trên luồng nền nên chờ tối đa 5 giây. */
    private static <A extends android.app.Activity> boolean waitFor(ActivityScenario<A> scenario,
                                                                   Check<A> check) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            AtomicReference<Boolean> ok = new AtomicReference<>(false);
            scenario.onActivity(a -> ok.set(check.test(a)));
            if (ok.get()) return true;
            Thread.sleep(100);
        }
        return false;
    }
}
